/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.workflows.workflowTask

import com.jayway.jsonpath.Criteria
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.ExistingServiceProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewServiceProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workflows.Channel
import io.infinitic.common.workflows.WorkflowDispatcher
import io.infinitic.common.workflows.data.channels.ChannelFilter
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.DispatchNewMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskCommand
import io.infinitic.common.workflows.data.commands.InlineTaskCommand
import io.infinitic.common.workflows.data.commands.InlineTaskPastCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalCommand
import io.infinitic.common.workflows.data.commands.SendSignalCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerCommand
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus
import io.infinitic.common.workflows.data.steps.StepStatus.Canceled
import io.infinitic.common.workflows.data.steps.StepStatus.Completed
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyFailed
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyTimedOut
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.TimedOut
import io.infinitic.common.workflows.data.steps.StepStatus.Unknown
import io.infinitic.common.workflows.data.steps.StepStatus.Waiting
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.exceptions.DeferredCanceledException
import io.infinitic.exceptions.DeferredFailedException
import io.infinitic.exceptions.DeferredTimedOutException
import io.infinitic.exceptions.DeferredUnknownException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.InvalidRunningTaskException
import io.infinitic.exceptions.workflows.MultipleCustomIdException
import io.infinitic.exceptions.workflows.WorkflowChangedException
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.WorkflowCheckMode
import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant

internal class WorkflowDispatcherImpl(
  private val workflowCheckMode: WorkflowCheckMode,
  private val workflowTaskParameters: WorkflowTaskParameters
) : WorkflowDispatcher {

  private val logger = KotlinLogging.logger {}

  // function used to set properties on workflow
  lateinit var setProperties: (Map<PropertyName, PropertyHash>) -> Unit

  // new commands discovered during execution of the method
  var newCommands: MutableList<PastCommand> = mutableListOf()

  // new step discovered during execution the method
  var newStep: NewStep? = null

  // position in the current method processing
  private var positionInMethod = PositionInWorkflowMethod()

  // current workflowTaskIndex (useful to retrieve status of Deferred)
  private var workflowTaskIndex = workflowTaskParameters.workflowMethod.workflowTaskIndexAtStart

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R =
      when (handler is ExistingWorkflowProxyHandler<*> && handler.isChannelGetter()) {
        true -> ChannelProxyHandler<SendChannel<*>>(handler).stub() as R
        false -> dispatch<R>(handler, true).await()
      }

  // asynchronous call: dispatch(stub::method)(*args)
  override fun <R : Any?> dispatch(handler: ProxyHandler<*>, clientWaiting: Boolean): Deferred<R> =
      when (handler) {
        is NewServiceProxyHandler -> handler.dispatchTask()
        is NewWorkflowProxyHandler -> handler.dispatchWorkflow()
        is ExistingServiceProxyHandler -> throw InvalidRunningTaskException("${handler.stub()}")
        is ExistingWorkflowProxyHandler -> handler.dispatchMethod()
        is ChannelProxyHandler -> handler.dispatchSignal()
      }

  /** Inlined task */
  override fun <S> inline(task: () -> S): S {
    // increment position
    nextPosition()

    @Suppress("UNCHECKED_CAST")
    return when (val pastCommand = getPastCommandAtCurrentPosition()) {
      null -> {
        // run inline task, checking that no proxy are used inside
        val value = ProxyHandler.inline(task)
        // record result
        val command = InlineTaskCommand()

        newCommands.add(
            InlineTaskPastCommand(
                commandId = getCurrentCommandId(),
                commandPosition = positionInMethod,
                commandSimpleName = InlineTaskCommand.simpleName(),
                commandStatus = CommandStatus.Completed(
                    returnValue = ReturnValue.from(value),
                    completionWorkflowTaskIndex = workflowTaskIndex,
                ),
                command = command,
            ),
        )
        // returns value
        value
      }

      else -> when (pastCommand.commandSimpleName == InlineTaskCommand.simpleName()) {
        true ->
          when (val status = pastCommand.commandStatus) {
            is CommandStatus.Completed -> status.returnValue.value() as S
            else -> thisShouldNotHappen()
          }

        else -> throwCommandsChangedException(pastCommand, null)
      }
    }
  }

  /** Deferred await() */
  @Suppress("UNCHECKED_CAST")
  override fun <T> await(deferred: Deferred<T>): T {
    // increment position
    nextPosition()

    // create a new step
    val newStep = NewStep(step = deferred.step, stepPosition = positionInMethod)

    deferred.step.checkAwaitIndex()

    val result = when (val pastStep = getSimilarPastStep(newStep)) {
      // this step is not found in the history
      null -> {
        // determine this step status based on history
        when (val stepStatus = newStep.step.statusAt(workflowTaskIndex)) {
          // we do not know the status of this step based on history
          is Waiting -> {
            this.newStep = newStep

            throw NewStepException
          }
          // we already know the status of this step based on history
          is Unknown,
          is Canceled,
          is Failed,
          is CurrentlyFailed,
          is TimedOut,
          is CurrentlyTimedOut -> {
            throw getDeferredException(stepStatus)
          }

          is Completed -> {
            stepStatus.returnValue.value() as T
          }
        }
      }
      // this step is already in the history
      else -> {
        val stepStatus = pastStep.stepStatus

        // if still ongoing, we stop here
        if (stepStatus == Waiting) throw KnownStepException

        // instance properties are now as when this deferred was terminated
        setProperties(pastStep.propertiesNameHashAtTermination ?: thisShouldNotHappen())

        // return deferred value
        when (stepStatus) {
          is Waiting -> {
            thisShouldNotHappen()
          }

          is Unknown -> {
            // workflowTaskIndex is now the one where this deferred was unknowing
            workflowTaskIndex = stepStatus.unknowingWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is Canceled -> {
            // workflowTaskIndex is now the one where this deferred was canceled
            workflowTaskIndex = stepStatus.cancellationWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is CurrentlyFailed -> {
            // workflowTaskIndex is now the one where this deferred was failed
            workflowTaskIndex = stepStatus.failureWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is Failed -> {
            // workflowTaskIndex is now the one where this deferred was failed
            workflowTaskIndex = stepStatus.failureWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is CurrentlyTimedOut -> {
            // workflowTaskIndex is now the one where this deferred was failed
            workflowTaskIndex = stepStatus.timeoutWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is TimedOut -> {
            // workflowTaskIndex is now the one where this deferred was failed
            workflowTaskIndex = stepStatus.timeoutWorkflowTaskIndex

            throw getDeferredException(stepStatus)
          }

          is Completed -> {
            // workflowTaskIndex is now the one where this deferred was completed
            workflowTaskIndex = stepStatus.completionWorkflowTaskIndex

            stepStatus.returnValue.value() as T
          }
        }
      }
    }

    deferred.step.nextAwaitIndex()

    return result
  }

  /** Deferred status */
  override fun <T> status(deferred: Deferred<T>): DeferredStatus =
      when (deferred.step.statusAt(workflowTaskIndex)) {
        is Waiting -> DeferredStatus.ONGOING
        is Unknown -> DeferredStatus.UNKNOWN
        is Completed -> DeferredStatus.COMPLETED
        is Canceled -> DeferredStatus.CANCELED
        is CurrentlyFailed,
        is Failed -> DeferredStatus.FAILED

        is CurrentlyTimedOut,
        is TimedOut -> DeferredStatus.TIMED_OUT
      }

  override fun timer(duration: JavaDuration): Deferred<JavaInstant> =
      dispatchCommand(
          StartDurationTimerCommand(MillisDuration(duration.toMillis())),
          StartDurationTimerCommand.simpleName(),
      )

  override fun timer(instant: JavaInstant): Deferred<JavaInstant> =
      dispatchCommand(
          StartInstantTimerCommand(MillisInstant(instant.toEpochMilli())),
          StartInstantTimerCommand.simpleName(),
      )

  override fun <S : T, T : Any> receive(
    channel: Channel<T>,
    klass: Class<S>?,
    limit: Int?,
    jsonPath: String?,
    criteria: Criteria?
  ): Deferred<S> = dispatchCommand(
      ReceiveSignalCommand(
          ChannelName(channel.name),
          klass?.let { ChannelType.from(it) },
          ChannelFilter.from(jsonPath, criteria),
          limit,
      ),
      ReceiveSignalCommand.simpleName(),
  )

  override fun <T : Any> send(channel: Channel<T>, signal: T) {
    dispatchCommand<T>(
        SendSignalCommand(
            workflowName = workflowTaskParameters.workflowName,
            workflowId = workflowTaskParameters.workflowId,
            workflowTag = null,
            channelName = ChannelName(channel.name),
            channelTypes = ChannelType.allFrom(signal::class.java),
            signalData = SignalData.from(signal),
        ),
        SendSignalCommand.simpleName(),
    )
  }

  /**
   * Deterministic generation of the current command id based on the time of workflowTaskInstant
   * and some unique characteristics of the command
   *
   * Being deterministic provides 2 benefits:
   * - commandId can be used as an idempotency key when processing tasks and workflows
   * - it avoids messing up the workflow state if a workflow task is processed twice,
   * triggering signals, tasks, workflows twice
   **/
  private fun getCurrentCommandId(): CommandId =
      when (val instant = workflowTaskParameters.workflowTaskInstant) {
        // before 0.13.0 workflowTaskInstant is null, we roll back to previous implementation
        // this workflow task will be eventually rejected by the workflow engine
        null -> CommandId()

        else -> {
          val str = "workflowId=${workflowTaskParameters.workflowId}" +
              "workflowMethodId=${workflowTaskParameters.workflowMethod.workflowMethodId}" +
              "workflowVersion=${workflowTaskParameters.workflowVersion}" +
              "positionInMethod=$positionInMethod"

          CommandId(IdGenerator.from(instant, str))
        }
      }

  /** Go to next position within the same branch */
  private fun nextPosition() {
    positionInMethod = positionInMethod.next()
  }

  /** Task dispatching */
  private fun <R : Any?> NewServiceProxyHandler<*>.dispatchTask(): Deferred<R> =
      dispatchCommand(
          DispatchTaskCommand(
              serviceName = serviceName,
              methodParameters = methodParameters,
              methodParameterTypes = methodParameterTypes,
              methodName = methodName,
              methodTimeout = timeoutInMillisDuration.getOrThrow(),
              taskTags = taskTags,
              taskMeta = taskMeta,
          ),
          CommandSimpleName(fullMethodName),
      )

  /** Workflow dispatching */
  private fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchWorkflow(): Deferred<R> =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          // it's not possible to have multiple customIds in tags
          if (workflowTags.count { it.isCustomId() } > 1) {
            throw MultipleCustomIdException
          }

          dispatchCommand(
              DispatchNewWorkflowCommand(
                  workflowName = workflowName,
                  methodName = methodName,
                  methodParameterTypes = methodParameterTypes,
                  methodParameters = methodParameters,
                  methodTimeout = timeoutInMillisDuration.getOrThrow(),
                  workflowTags = workflowTags,
                  workflowMeta = workflowMeta,
              ),
              CommandSimpleName(fullMethodName),
          )
        }
      }

  /** Method dispatching */
  private fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethod(): Deferred<R> =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> dispatchCommand(
            DispatchNewMethodCommand(
                workflowName = workflowName,
                workflowId = requestBy.workflowId,
                workflowTag = requestBy.workflowTag,
                methodName = methodName,
                methodParameterTypes = methodParameterTypes,
                methodParameters = methodParameters,
                methodTimeout = timeoutInMillisDuration.getOrThrow(),
            ),
            CommandSimpleName(fullMethodName),
        )
      }

  /** Signal dispatching */
  private fun <R : Any?> ChannelProxyHandler<*>.dispatchSignal(): Deferred<R> {
    if (methodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

    return dispatchCommand(
        SendSignalCommand(
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            workflowTag = requestBy.workflowTag,
            channelName = channelName,
            channelTypes = channelTypes,
            signalData = signalData,
        ),
        SendSignalCommand.simpleName(),
    )
  }

  private fun <S> dispatchCommand(
    command: Command,
    commandSimpleName: CommandSimpleName
  ): Deferred<S> {
    // increment position
    nextPosition()

    // create instruction that will be sent to engine
    // if it does not already exist in the history
    val newCommand = PastCommand.from(
        commandId = getCurrentCommandId(),
        commandPosition = positionInMethod,
        commandSimpleName = commandSimpleName,
        commandStatus = CommandStatus.Ongoing,
        command = command,
    )

    // do we know the same command from the history?
    val pastCommand = getSimilarPastCommand(newCommand)

    return if (pastCommand == null) {
      // if this is a new command, we add it to the newCommands list
      newCommands.add(newCommand)
      // and returns a Deferred with an ongoing step
      Deferred<S>(Step.Id.from(newCommand)).apply {
        this.workflowDispatcher = this@WorkflowDispatcherImpl
      }
    } else {
      // else returns a Deferred linked to pastCommand
      Deferred<S>(Step.Id.from(pastCommand)).apply {
        this.workflowDispatcher = this@WorkflowDispatcherImpl
      }
    }
  }

  private fun getPastCommandAtCurrentPosition(): PastCommand? = workflowTaskParameters
      .workflowMethod.pastCommands.find { it.commandPosition == positionInMethod }

  private fun throwCommandsChangedException(
    pastCommand: PastCommand?,
    newCommand: PastCommand?
  ): Nothing {
    val e = WorkflowChangedException(
        "${workflowTaskParameters.workflowName}",
        "${workflowTaskParameters.workflowMethod.methodName}",
        "$positionInMethod",
    )
    logger.error(e) {
      """Workflow ${workflowTaskParameters.workflowId}:past and new commands are different
            |pastCommand = ${pastCommand?.command}
            |newCommand  = ${newCommand?.command}
            |workflowCheckMode = $workflowCheckMode
            """
          .trimMargin()
    }
    throw e
  }

  private fun getSimilarPastCommand(newCommand: PastCommand): PastCommand? {
    // find pastCommand in current position
    val pastCommand = getPastCommandAtCurrentPosition()
    // if it exists, check it has not changed
    if (pastCommand != null && !pastCommand.isSameThan(newCommand, workflowCheckMode)) {
      throwCommandsChangedException(pastCommand, newCommand)
    }

    return pastCommand
  }

  private fun getSimilarPastStep(newStep: NewStep): PastStep? {
    // Do we already know a step in this position?
    val currentStep = workflowTaskParameters.workflowMethod.currentStep
    val pastStep = when (currentStep?.stepPosition) {
      positionInMethod -> currentStep
      else -> workflowTaskParameters.workflowMethod.pastSteps.find { it.stepPosition == positionInMethod }
    }

    // if it exists, check it has not changed
    if (pastStep != null && !pastStep.isSameThan(newStep)) {
      val e = WorkflowChangedException(
          workflowTaskParameters.workflowName.name,
          "${workflowTaskParameters.workflowMethod.methodName}",
          "$positionInMethod",
      )
      logger.error(e) {
        """Workflow ${workflowTaskParameters.workflowId}: past and new steps are different
                |pastStep = $pastStep
                |newStep = $newStep
                """
            .trimMargin()
      }
      throw e
    }

    return pastStep
  }

  /** Exception when waiting a deferred */
  private fun getDeferredException(stepStatus: StepStatus) =
      when (stepStatus) {
        is Unknown -> DeferredUnknownException.from(stepStatus.deferredUnknownError)
        is Canceled -> DeferredCanceledException.from(stepStatus.deferredCanceledError)
        is CurrentlyFailed -> DeferredFailedException.from(stepStatus.deferredFailedError)
        is Failed -> DeferredFailedException.from(stepStatus.deferredFailedError)
        is CurrentlyTimedOut -> DeferredTimedOutException.from(stepStatus.deferredTimedOutError)
        is TimedOut -> DeferredTimedOutException.from(stepStatus.deferredTimedOutError)
        is Completed, Waiting -> thisShouldNotHappen()
      }
}
