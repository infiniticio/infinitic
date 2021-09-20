/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workflows.workflowTask

import com.jayway.jsonpath.Criteria
import io.infinitic.common.data.JobOptions
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.Name
import io.infinitic.common.proxies.ChannelInstanceProxyHandler
import io.infinitic.common.proxies.ChannelSelectionProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskInstanceProxyHandler
import io.infinitic.common.proxies.TaskSelectionProxyHandler
import io.infinitic.common.proxies.WorkflowInstanceProxyHandler
import io.infinitic.common.proxies.WorkflowSelectionProxyHandler
import io.infinitic.common.proxies.data.Method
import io.infinitic.common.proxies.data.TaskInstance
import io.infinitic.common.proxies.data.WorkflowInstance
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.channels.ChannelEventFilter
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandReturnValue
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflows.data.commands.DispatchTask
import io.infinitic.common.workflows.data.commands.EndAsync
import io.infinitic.common.workflows.data.commands.EndInlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.ReceiveInChannel
import io.infinitic.common.workflows.data.commands.SendToChannel
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartDurationTimer
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.commands.StartInstantTimer
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus.Canceled
import io.infinitic.common.workflows.data.steps.StepStatus.Completed
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.OngoingFailure
import io.infinitic.common.workflows.data.steps.StepStatus.Waiting
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.DispatchTaskSelectionException
import io.infinitic.exceptions.clients.UseChannelOnNewWorkflowException
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.exceptions.workflows.CanceledDeferredException
import io.infinitic.exceptions.workflows.FailedDeferredException
import io.infinitic.exceptions.workflows.ShouldNotUseAsyncFunctionInsideInlinedTaskException
import io.infinitic.exceptions.workflows.ShouldNotWaitInsideInlinedTaskException
import io.infinitic.exceptions.workflows.WorkflowUpdatedWhileRunningException
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.WorkflowDispatcher
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant

internal class WorkflowDispatcherImpl(
    private val workflowTaskParameters: WorkflowTaskParameters,
    private val setProperties: (Map<PropertyHash, PropertyValue>, Map<PropertyName, PropertyHash>) -> Unit
) : WorkflowDispatcher {

    private val logger = KotlinLogging.logger {}

    // position in the current method processing
    private var methodRunIndex = MethodRunIndex()

    // current workflowTaskIndex (useful to retrieve status of Deferred)
    private var workflowTaskIndex = workflowTaskParameters.methodRun.workflowTaskIndexAtStart

    // new commands discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps discovered during execution the method (possibly more than one due to `async` function)
    var newSteps: MutableList<NewStep> = mutableListOf()

    // asynchronous call: dispatch(stub::method)(*args)
    override fun <R : Any?> dispatch(
        handler: ProxyHandler<*>,
        clientWaiting: Boolean,
        tags: Set<String>?,
        options: JobOptions?,
        meta: Map<String, ByteArray>?,
    ): Deferred<R> = when (handler) {
        is TaskInstanceProxyHandler ->
            dispatchNewTask(
                handler.instance(),
                handler.method(),
                handler.simpleName,
                tags?.map { TaskTag(it) }?.toSet() ?: handler.taskTags,
                (options as TaskOptions?) ?: handler.taskOptions,
                meta?.run { TaskMeta(this) } ?: handler.taskMeta
            )
        is WorkflowInstanceProxyHandler -> when (handler.isMethodChannel()) {
            true -> throw UseChannelOnNewWorkflowException(handler.klass.name)
            false -> dispatchNewWorkflow(
                handler.instance(),
                handler.method(),
                handler.simpleName,
                tags?.map { WorkflowTag(it) }?.toSet() ?: handler.workflowTags,
                (options as WorkflowOptions?) ?: handler.workflowOptions,
                meta?.run { WorkflowMeta(this) } ?: handler.workflowMeta
            )
        }
        is TaskSelectionProxyHandler ->
            throw DispatchTaskSelectionException(handler.klass.name, "dispatch")
        is WorkflowSelectionProxyHandler ->
            when (handler.isMethodChannel()) {
                // special case of getting a channel from a workflow
                true -> TODO("Not Yet Implemented")
                false -> TODO("Not Yet Implemented")
                // dispatchWorkflowMethod(handler.selection(), handler.method(), clientWaiting)
            }
        is ChannelSelectionProxyHandler ->
            TODO("Not yet implemented")
        is ChannelInstanceProxyHandler -> TODO()
    }
    /*
     * Async Branch dispatching
     */
    override fun <S> async(branch: () -> S): Deferred<S> {
        // increment position
        positionNext()

        // create instruction that will be sent to engine only if new
        val newCommand = NewCommand(
            command = StartAsync,
            commandName = null,
            commandSimpleName = CommandSimpleName("${CommandType.START_ASYNC}"),
            commandPosition = methodRunIndex.methodPosition
        )

        val pastCommand = getSimilarPastCommand(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)

            // returns a Deferred with an ongoing step
            return Deferred(Step.Id.from(newCommand), this)
        }

        // async branch is processed only if on path of targetPosition
        if (methodRunIndex.leadsTo(workflowTaskParameters.targetPosition)) {
            // update workflowTaskIndex to value linked to first processing of this branch
            workflowTaskIndex = pastCommand.workflowTaskIndexAtStart!!

            // update workflow instance properties
            setProperties(
                workflowTaskParameters.workflowPropertiesHashValue,
                pastCommand.propertiesNameHashAtStart!!
            )

            // go down
            positionDown()

            // exceptions caught by runMethod
            val commandOutput = CommandReturnValue.from(branch())

            // go up
            positionUp()

            newCommands.add(
                NewCommand(
                    command = EndAsync(commandOutput),
                    commandName = null,
                    commandSimpleName = CommandSimpleName("${CommandType.END_ASYNC}"),
                    commandPosition = methodRunIndex.methodPosition
                )
            )

            // async is completed
            throw AsyncCompletedException
        }

        // returns a Deferred linked to pastCommand
        return Deferred(Step.Id.from(pastCommand), this)
    }

    /*
     * Inlined task
     */
    override fun <S> inline(task: () -> S): S {
        // increment position
        positionNext()

        // create instruction that will be sent to engine only if new
        val startCommand = NewCommand(
            command = StartInlineTask,
            commandName = null,
            commandSimpleName = CommandSimpleName("${CommandType.START_INLINE_TASK}"),
            commandPosition = methodRunIndex.methodPosition
        )

        val pastCommand = getSimilarPastCommand(startCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(startCommand)
            // go down (in case this inline task asynchronously dispatches some tasks)
            positionDown()
            // run inline task
            val commandOutput = try { CommandReturnValue.from(task()) } catch (e: Exception) {
                when (e) {
                    is NewStepException, is KnownStepException -> throw ShouldNotWaitInsideInlinedTaskException(workflowTaskParameters.getFullMethodName())
                    is AsyncCompletedException -> throw ShouldNotUseAsyncFunctionInsideInlinedTaskException(workflowTaskParameters.getFullMethodName())
                    else -> throw e
                }
            }
            // go up
            positionUp()
            // record result
            val endCommand = NewCommand(
                command = EndInlineTask(commandOutput),
                commandName = null,
                commandSimpleName = CommandSimpleName("${CommandType.END_INLINE_TASK}"),
                commandPosition = methodRunIndex.methodPosition
            )
            newCommands.add(endCommand)
            // returns a Deferred with an ongoing step
            @Suppress("UNCHECKED_CAST")
            return commandOutput.get() as S
        } else {
            @Suppress("UNCHECKED_CAST")
            return when (val status = pastCommand.commandStatus) {
                is CommandStatus.Completed -> status.returnValue.get() as S
                else -> thisShouldNotHappen("inline task with status $status")
            }
        }
    }

    /*
     * Deferred await()
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> await(deferred: Deferred<T>): T {
        // increment position
        positionNext()

        // create a new step
        val newStep = NewStep(
            step = deferred.step,
            stepPosition = methodRunIndex.methodPosition
        )
        val pastStep = getSimilarPastStep(newStep)

        // new step (e.g. a logical combination of previous deferred)
        if (pastStep == null) {

            // determine status
            deferred.stepStatus = newStep.step.statusAt(workflowTaskIndex)

            return when (val stepStatus = deferred.stepStatus) {
                is Completed -> stepStatus.returnValue.get() as T
                is Waiting -> {
                    // add this step
                    newSteps.add(newStep)

                    throw NewStepException
                }
                is Canceled -> throw CanceledDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
                is Failed -> throw FailedDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
                is OngoingFailure -> throw FailedDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
            }
        }

        // known step
        val stepStatus = pastStep.stepStatus

        // update deferred status
        deferred.stepStatus = stepStatus

        // if still ongoing, we stop here
        if (stepStatus == Waiting) throw KnownStepException

        // instance properties are now as when this deferred was terminated
        setProperties(
            workflowTaskParameters.workflowPropertiesHashValue,
            pastStep.propertiesNameHashAtTermination!!
        )

        // return deferred value
        return when (stepStatus) {
            is Waiting -> thisShouldNotHappen()
            is Completed -> {
                // workflowTaskIndex is now the one where this deferred was completed
                workflowTaskIndex = stepStatus.completionWorkflowTaskIndex

                stepStatus.returnValue.get() as T
            }
            is Canceled -> {
                // workflowTaskIndex is now the one where this deferred was canceled
                workflowTaskIndex = stepStatus.cancellationWorkflowTaskIndex

                throw CanceledDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
            }
            is Failed -> {
                // workflowTaskIndex is now the one where this deferred was failed
                workflowTaskIndex = stepStatus.failureWorkflowTaskIndex

                throw FailedDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
            }
            is OngoingFailure -> {
                // workflowTaskIndex is now the one where this deferred was failed
                workflowTaskIndex = stepStatus.failureWorkflowTaskIndex

                throw FailedDeferredException(
                    getCommandName(stepStatus.commandId)?.toString(),
                    stepStatus.commandId.id
                )
            }
        }
    }

    /*
     * Deferred status()
     */
    override fun <T> status(deferred: Deferred<T>): DeferredStatus =
        when (deferred.step.statusAt(workflowTaskIndex)) {
            is Waiting -> DeferredStatus.ONGOING
            is Completed -> DeferredStatus.COMPLETED
            is Canceled -> DeferredStatus.CANCELED
            is Failed -> DeferredStatus.FAILED
            is OngoingFailure -> DeferredStatus.FAILED
        }

    /*
     * Task dispatching
     */
    private fun <R : Any?> dispatchNewTask(
        task: TaskInstance,
        method: Method,
        simpleName: String,
        tags: Set<TaskTag>,
        options: TaskOptions,
        meta: TaskMeta
    ): Deferred<R> = dispatchCommand(
        DispatchTask(
            taskName = task.taskName,
            methodParameters = method.methodParameters,
            methodParameterTypes = method.methodParameterTypes,
            methodName = method.methodName,
            taskTags = tags,
            taskOptions = options,
            taskMeta = meta
        ),
        CommandSimpleName(simpleName)
    )

    /*
     * Workflow dispatching
     */
    private fun <R : Any?> dispatchNewWorkflow(
        workflow: WorkflowInstance,
        method: Method,
        simpleName: String,
        tags: Set<WorkflowTag>,
        options: WorkflowOptions,
        meta: WorkflowMeta
    ): Deferred<R> = dispatchCommand(
        DispatchChildWorkflow(
            childWorkflowName = workflow.workflowName,
            childMethodName = method.methodName,
            childMethodParameterTypes = method.methodParameterTypes,
            childMethodParameters = method.methodParameters,
            workflowTags = tags,
            workflowOptions = options,
            workflowMeta = meta,
        ),
        CommandSimpleName(simpleName)
    )

    /*
     * Start_Duration_Timer command dispatching
     */
    override fun timer(duration: JavaDuration): Deferred<JavaInstant> = dispatchCommand(
        StartDurationTimer(MillisDuration(duration.toMillis())),
        CommandSimpleName("${CommandType.START_DURATION_TIMER}")
    )

    /*
     * Start_Instant_Timer command dispatching
     */
    override fun timer(instant: JavaInstant): Deferred<JavaInstant> = dispatchCommand(
        StartInstantTimer(MillisInstant(instant.toEpochMilli())),
        CommandSimpleName("${CommandType.START_INSTANT_TIMER}")
    )

    /*
     * Receive_From_Channel command
     */
    override fun <T : Any> receiveFromChannel(
        channel: ChannelImpl<T>,
        jsonPath: String?,
        criteria: Criteria?
    ): Deferred<T> = dispatchCommand(
        ReceiveInChannel(
            ChannelName(channel.getNameOrThrow()),
            null,
            ChannelEventFilter.from(jsonPath, criteria)

        ),
        CommandSimpleName("${CommandType.RECEIVE_IN_CHANNEL}")
    )

    /*
     * Receive_From_Channel command with channelEventType
     */
    override fun <S : T, T : Any> receiveFromChannel(
        channel: ChannelImpl<T>,
        klass: Class<S>,
        jsonPath: String?,
        criteria: Criteria?
    ): Deferred<S> = dispatchCommand(
        ReceiveInChannel(
            ChannelName(channel.getNameOrThrow()),
            ChannelSignalType.from(klass),
            ChannelEventFilter.from(jsonPath, criteria)
        ),
        CommandSimpleName("${CommandType.RECEIVE_IN_CHANNEL}")
    )

    /*
     * Sent_to_Channel command dispatching
     */
    override fun <T : Any> sendToChannel(channel: ChannelImpl<T>, event: T): CompletableFuture<Unit> {
        dispatchCommand<T>(
            SendToChannel(
                ChannelName(channel.getNameOrThrow()),
                ChannelSignal.from(event),
                ChannelSignalType.allFrom(event::class.java)
            ),
            CommandSimpleName("${CommandType.SENT_TO_CHANNEL}")
        )

        return CompletableFuture.completedFuture(Unit)
    }

    /*
     * Go to next position within the same branch
     */
    private fun positionNext() {
        methodRunIndex = methodRunIndex.next()
    }

    /*
     * End of a async { ... } function
     */
    private fun positionUp() {
        methodRunIndex.up()?.let { methodRunIndex = it }
    }

    /*
     * Start of a async { ... } function
     */
    private fun positionDown() {
        methodRunIndex = methodRunIndex.down()
    }

    private fun <S> dispatchCommand(command: Command, commandSimpleName: CommandSimpleName): Deferred<S> {
        // increment position
        positionNext()

        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = command,
            commandName = when (command) {
                is DispatchTask -> command.taskName
                is DispatchChildWorkflow -> command.childWorkflowName
                else -> null
            },
            commandSimpleName = commandSimpleName,
            commandPosition = methodRunIndex.methodPosition
        )

        val pastCommand = getSimilarPastCommand(newCommand)

        return if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // and returns a Deferred with an ongoing step
            Deferred(Step.Id.from(newCommand), this)
        } else {
            // else returns a Deferred linked to pastCommand
            Deferred(Step.Id.from(pastCommand), this)
        }
    }

    private fun getSimilarPastCommand(newCommand: NewCommand): PastCommand? {
        // find pastCommand in current position
        val pastCommand = workflowTaskParameters.methodRun.pastCommands
            .find { it.commandPosition == methodRunIndex.methodPosition }

        // if it exists, check it has not changed
        if (pastCommand != null && !pastCommand.isSameThan(newCommand, workflowTaskParameters.workflowOptions.workflowChangeCheckMode)) {
            logger.error { "pastCommand =  $pastCommand" }
            logger.error { "newCommand =  $newCommand" }
            logger.error { "workflowChangeCheckMode = ${workflowTaskParameters.workflowOptions.workflowChangeCheckMode}" }
            throw WorkflowUpdatedWhileRunningException(
                workflowTaskParameters.workflowName.name,
                "${workflowTaskParameters.methodRun.methodName}",
                "${methodRunIndex.methodPosition}"
            )
        }

        return pastCommand
    }

    private fun getSimilarPastStep(newStep: NewStep): PastStep? {
        // find pastCommand in current position
        val pastStep = workflowTaskParameters.methodRun.pastSteps
            .find { it.stepPosition == methodRunIndex.methodPosition }

        // if it exists, check it has not changed
        if (pastStep != null && !pastStep.isSimilarTo(newStep)) {
            logger.error { "pastStep = $pastStep" }
            logger.error { "newStep = $newStep" }
            throw WorkflowUpdatedWhileRunningException(
                workflowTaskParameters.workflowName.name,
                "${workflowTaskParameters.methodRun.methodName}",
                "${methodRunIndex.methodPosition}"
            )
        }

        return pastStep
    }

    private fun getCommandName(commandId: CommandId): Name? = workflowTaskParameters.methodRun
        .pastCommands.firstOrNull { it.commandId == commandId }?.commandName
        ?: newCommands.firstOrNull { it.commandId == commandId }?.commandName
}
