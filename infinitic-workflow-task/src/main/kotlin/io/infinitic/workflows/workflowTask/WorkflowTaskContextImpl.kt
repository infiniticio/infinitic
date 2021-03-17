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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandReturnValue
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.commands.CommandStatusCanceled
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
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
import io.infinitic.common.workflows.data.steps.StepStatusCanceled
import io.infinitic.common.workflows.data.steps.StepStatusCompleted
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.exceptions.NoMethodCallAtAsync
import io.infinitic.exceptions.ShouldNotUseAsyncFunctionInsideInlinedTask
import io.infinitic.exceptions.ShouldNotWaitInsideInlinedTask
import io.infinitic.exceptions.SuspendMethodNotSupported
import io.infinitic.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.WorkflowTaskContext
import org.slf4j.LoggerFactory
import java.lang.reflect.Proxy
import kotlin.reflect.jvm.kotlinFunction
import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant

internal class WorkflowTaskContextImpl(
    private val workflowTaskParameters: WorkflowTaskParameters,
    private val setProperties: (Map<PropertyHash, PropertyValue>, Map<PropertyName, PropertyHash>) -> Unit
) : WorkflowTaskContext {
    private val logger = LoggerFactory.getLogger(javaClass)

    // position in the current method processing
    private var methodRunIndex = MethodRunIndex()

    // current workflowTaskIndex (useful to retrieve status of Deferred)
    private var workflowTaskIndex = workflowTaskParameters.methodRun.workflowTaskIndexAtStart

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps (if any) discovered during execution the method (can be multiple due to `async` function)
    var newSteps: MutableList<NewStep> = mutableListOf()

    /*
     * Async Task dispatching
     */
    override fun <T : Any, S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> = when (val handler = Proxy.getInvocationHandler(proxy)) {
        is NewTaskProxyHandler<*> -> {
            handler.isSync = false
            proxy.method()
            dispatchTask(handler)
        }
        is NewWorkflowProxyHandler<*> -> {
            handler.isSync = false
            proxy.method()
            dispatchWorkflow(handler)
        }
        else -> throw RuntimeException("Not Yet Implemented")
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
                    commandSimpleName = CommandSimpleName("${CommandType.END_ASYNC}"),
                    commandPosition = methodRunIndex.methodPosition
                )
            )

            // async is completed
            throw AsyncCompletedException()
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
                    is NewStepException, is KnownStepException -> throw ShouldNotWaitInsideInlinedTask(workflowTaskParameters.getFullMethodName())
                    is AsyncCompletedException -> throw ShouldNotUseAsyncFunctionInsideInlinedTask(workflowTaskParameters.getFullMethodName())
                    else -> throw e
                }
            }
            // go up
            positionUp()
            // record result
            val endCommand = NewCommand(
                command = EndInlineTask(commandOutput),
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
                is CommandStatusOngoing -> throw RuntimeException("This should not happen: uncompleted inline task")
                is CommandStatusCanceled -> throw RuntimeException("This should not happen: canceled inline task")
                is CommandStatusCompleted -> status.returnValue.get() as S
            }
        }
    }

    /*
     * Deferred await()
     */
    override fun <T> await(deferred: Deferred<T>): T {
        // increment position
        positionNext()

        // create a new step
        val newStep = NewStep(
            step = deferred.step,
            stepPosition = methodRunIndex.methodPosition
        )
        val pastStep = getSimilarPastStep(newStep)

        // if this is really a new step, we check its status based on current workflow message index
        if (pastStep == null) {
            deferred.stepStatus = newStep.step.stepStatusAtWorkflowTaskIndex(workflowTaskIndex)
            // if this deferred is still ongoing,
            if (deferred.stepStatus is StepStatusOngoing) {
                // we add a new step
                newSteps.add(newStep)
                // and stop here
                throw NewStepException()
            }
            // if this deferred is already terminated, we continue
            return result(deferred)
        }

        // set status
        deferred.stepStatus = pastStep.stepStatus

        // throw KnownStepException if ongoing else else update message index
        workflowTaskIndex = when (val stepStatus = deferred.stepStatus) {
            is StepStatusOngoing -> throw KnownStepException()
            is StepStatusCompleted -> stepStatus.completionWorkflowTaskIndex
            is StepStatusCanceled -> stepStatus.cancellationWorkflowTaskIndex
        }

        // update workflow instance properties
        setProperties(
            workflowTaskParameters.workflowPropertiesHashValue,
            pastStep.propertiesNameHashAtTermination!!
        )

        // continue
        return result(deferred)
    }

    /*
     * Deferred status()
     */
    override fun <T> status(deferred: Deferred<T>): DeferredStatus = when (deferred.step.stepStatusAtWorkflowTaskIndex(workflowTaskIndex)) {
        is StepStatusOngoing -> DeferredStatus.ONGOING
        is StepStatusCompleted -> DeferredStatus.COMPLETED
        is StepStatusCanceled -> DeferredStatus.CANCELED
    }

    /*
     * Task dispatching
     */
    override fun <S> dispatchTask(handler: NewTaskProxyHandler<*>): Deferred<S> {
        val method = handler.method ?: throw NoMethodCallAtAsync(handler.klass.name)
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupported(handler.klass.name, method.name)

        val deferred = dispatchCommand<S>(
            DispatchTask.from(method, handler.args, handler.taskMeta, handler.taskOptions),
            CommandSimpleName.fromMethod(method)
        )
        handler.reset()
        return deferred
    }

    override fun <S> dispatchAndWait(handler: NewTaskProxyHandler<*>): S =
        dispatchTask<S>(handler).await()

    /*
     * Workflow dispatching
     */
    override fun <S> dispatchWorkflow(handler: NewWorkflowProxyHandler<*>): Deferred<S> {
        val method = handler.method ?: throw NoMethodCallAtAsync(handler.klass.name)
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupported(handler.klass.name, method.name)

        val deferred = dispatchCommand<S>(
            DispatchChildWorkflow.from(method, handler.args, handler.workflowMeta, handler.workflowOptions),
            CommandSimpleName.fromMethod(method)
        )
        handler.reset()
        return deferred
    }

    /*
     * Start another running child workflow
     */
    override fun <S> dispatchAndWait(handler: NewWorkflowProxyHandler<*>): S =
        dispatchWorkflow<S>(handler).await()

    /*
     * Process another running workflow's method
     */
    override fun <S> dispatchAndWait(handler: ExistingWorkflowProxyHandler<*>): S {
        TODO("Not yet implemented")
    }

    /*
     * Send event to another workflow's channel
     */
    override fun dispatchAndWait(handler: SendChannelProxyHandler<*>) {
        TODO("Not yet implemented")
    }

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
    override fun <T : Any> receiveFromChannel(channel: ChannelImpl<T>): Deferred<T> = dispatchCommand(
        ReceiveInChannel(ChannelName(channel.getNameOrThrow()), null),
        CommandSimpleName("${CommandType.RECEIVE_IN_CHANNEL}")
    )

    /*
     * Receive_From_Channel command with channelEventType
     */
    override fun <S : T, T : Any> receiveFromChannel(channel: ChannelImpl<T>, klass: Class<S>): Deferred<S> = dispatchCommand(
        ReceiveInChannel(ChannelName(channel.getNameOrThrow()), ChannelEventType.from(klass)),
        CommandSimpleName("${CommandType.RECEIVE_IN_CHANNEL}")
    )

    /*
     * Sent_to_Channel command dispatching
     */
    override fun <T : Any> sendToChannel(channel: ChannelImpl<T>, event: T) {
        dispatchCommand<T>(
            SendToChannel(
                ChannelName(channel.getNameOrThrow()),
                ChannelEvent.from(event),
                ChannelEventType.allFrom(event::class.java)
            ),
            CommandSimpleName("${CommandType.SENT_TO_CHANNEL}")
        )
    }

    /*
     * Get return value from Deferred
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> result(deferred: Deferred<T>): T = when (val status = deferred.stepStatus) {
        is StepStatusOngoing -> throw RuntimeException("This should not happen: reaching result of an ongoing deferred")
        is StepStatusCompleted -> status.completionResult.get() as T
        is StepStatusCanceled -> status.cancellationResult.get() as T
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
        if (pastCommand != null && !pastCommand.isSimilarTo(newCommand, workflowTaskParameters.workflowOptions.workflowChangeCheckMode)) {
            logger.error("pastCommand =  {}", pastCommand)
            logger.error("newCommand =  {}", newCommand)
            logger.error("workflowChangeCheckMode =  {}", workflowTaskParameters.workflowOptions.workflowChangeCheckMode)
            throw WorkflowUpdatedWhileRunning(
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
            logger.error("pastStep =  {}", pastStep)
            logger.error("newStep =  {}", newStep)
            throw WorkflowUpdatedWhileRunning(
                workflowTaskParameters.workflowName.name,
                "${workflowTaskParameters.methodRun.methodName}",
                "${methodRunIndex.methodPosition}"
            )
        }

        return pastStep
    }
}
