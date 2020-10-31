// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.worker.workflowTask

import io.infinitic.common.tasks.proxies.MethodProxyHandler
import io.infinitic.common.workflows.Deferred
import io.infinitic.common.workflows.DeferredStatus
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.WorkflowTaskContext
import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandOutput
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
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatusCanceled
import io.infinitic.common.workflows.data.steps.StepStatusCompleted
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.exceptions.NoMethodCallAtAsync
import io.infinitic.common.workflows.exceptions.ShouldNotUseAsyncFunctionInsideInlinedTask
import io.infinitic.common.workflows.exceptions.ShouldNotWaitInsideInlinedTask
import io.infinitic.common.workflows.exceptions.WorkflowDefinitionUpdatedWhileOngoing
import java.lang.reflect.Method

class WorkflowTaskContextImpl(
    private val workflowTaskInput: WorkflowTaskInput,
    private val workflow: Workflow
) : WorkflowTaskContext {
    // position in the current method processing
    private var methodRunIndex = MethodRunIndex()

    // current workflowTaskIndex (useful to retrieve status of Deferred)
    private var workflowTaskIndex = workflowTaskInput.methodRun.workflowTaskIndexAtStart

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps (if any) discovered during execution the method (can be multiple due to `async` function)
    var newSteps: MutableList<NewStep> = mutableListOf()

    init {
        // set workflowTask context
        workflow.context = this
    }

    /*
     * Async Task dispatching
     */
    override fun <T : Any, S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> {
        val command = Class.forName(proxy.toString())

        // get a proxy for T
        val handler = MethodProxyHandler(command)

        // get a proxy instance
        @Suppress("UNCHECKED_CAST")
        val klass = handler.instance() as T

        // this call will capture method and arguments
        klass.method()

        return dispatchTask<S>(
            handler.method ?: throw NoMethodCallAtAsync(command::class.java.name),
            handler.args
        )
    }

    /*
     * Async Workflow dispatching
     */
    override fun <T : Workflow, S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> {
        val command = Class.forName(proxy.toString())

        // get a proxy for T
        val handler = MethodProxyHandler(command)

        // get a proxy instance
        @Suppress("UNCHECKED_CAST")
        val klass = handler.instance() as T

        // this call will capture method and arguments
        klass.method()

        return dispatchWorkflow<S>(
            handler.method ?: throw NoMethodCallAtAsync(command::class.java.name),
            handler.args
        )
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

        val pastCommand = findPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)

            // returns a Deferred with an ongoing step
            return Deferred<S>(Step.Id.from(newCommand), this)
        }

        // async branch is processed only if on path of targetPosition
        if (methodRunIndex.leadsTo(workflowTaskInput.targetPosition)) {
            // update workflowTaskIndex to value linked to first processing of this branch
            workflowTaskIndex = pastCommand.workflowTaskIndexAtStart!!

            // update workflow instance properties
            setWorkflowProperties(
                workflow,
                workflowTaskInput.workflowPropertiesHashValue,
                pastCommand.propertiesNameHashAtStart
            )

            // go down
            positionDown()

            // exceptions caught by runMethod
            val commandOutput = CommandOutput.from(branch())

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
        return Deferred<S>(Step.Id.from(pastCommand), this)
    }

    /*
     * Inlined task
     */
    override fun <S> task(inline: () -> S): S {
        // increment position
        positionNext()

        // create instruction that will be sent to engine only if new
        val startCommand = NewCommand(
            command = StartInlineTask,
            commandSimpleName = CommandSimpleName("${CommandType.START_INLINE_TASK}"),
            commandPosition = methodRunIndex.methodPosition
        )

        val pastCommand = findPastCommandSimilarTo(startCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(startCommand)
            // go down (in case this inline task asynchronously dispatches some tasks)
            positionDown()
            // run inline task
            val commandOutput = try { CommandOutput.from(inline()) } catch (e: Exception) {
                when (e) {
                    is NewStepException, is KnownStepException -> throw ShouldNotWaitInsideInlinedTask(workflowTaskInput.getErrorMethodName())
                    is AsyncCompletedException -> throw ShouldNotUseAsyncFunctionInsideInlinedTask(workflowTaskInput.getErrorMethodName())
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
                is CommandStatusCompleted -> status.completionResult.get() as S
            }
        }
    }

    /*
     * Deferred await()
     */
    override fun <T> await(deferred: Deferred<T>): Deferred<T> {
        // increment position
        positionNext()

        // create a new step
        val newStep = NewStep(
            step = deferred.step,
            stepPosition = methodRunIndex.methodPosition
        )
        val pastStep = getPastStepSimilarTo(newStep)

        // if this is really a new step, we check its status based on current workflow message index
        if (pastStep == null) {
            deferred.stepStatus = newStep.step.stepStatusAtMessageIndex(workflowTaskIndex)
            // if this deferred is still ongoing,
            if (deferred.stepStatus is StepStatusOngoing) {
                // we add a new step
                newSteps.add(newStep)
                // and stop here
                throw NewStepException()
            }
            // if this deferred is already terminated, we continue
            return deferred
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
        setWorkflowProperties(
            workflow,
            workflowTaskInput.workflowPropertiesHashValue,
            pastStep.propertiesNameHashAtTermination
        )

        // continue
        return deferred
    }

    /*
     * Deferred result()
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> result(deferred: Deferred<T>): T = when (val status = await(deferred).stepStatus) {
        is StepStatusOngoing -> throw RuntimeException("This should not happen: reaching result of an ongoing deferred")
        is StepStatusCompleted -> status.completionResult.get() as T
        is StepStatusCanceled -> status.cancellationResult.get() as T
    }

    /*
     * Deferred status()
     */
    override fun <T> status(deferred: Deferred<T>): DeferredStatus = when (deferred.step.stepStatusAtMessageIndex(workflowTaskIndex)) {
        is StepStatusOngoing -> DeferredStatus.ONGOING
        is StepStatusCompleted -> DeferredStatus.COMPLETED
        is StepStatusCanceled -> DeferredStatus.CANCELED
    }

    /*
     * Task dispatching
     */
    override fun <S> dispatchTask(method: Method, args: Array<out Any>) =
        dispatch<S>(DispatchTask.from(method, args), CommandSimpleName.fromMethod(method))

    /*
     * Workflow dispatching
     */
    override fun <S> dispatchWorkflow(method: Method, args: Array<out Any>) =
        dispatch<S>(DispatchChildWorkflow.from(method, args), CommandSimpleName.fromMethod(method))

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

    private fun <S> dispatch(command: Command, commandSimpleName: CommandSimpleName): Deferred<S> {
        // increment position
        positionNext()

        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = command,
            commandSimpleName = commandSimpleName,
            commandPosition = methodRunIndex.methodPosition
        )

        val pastCommand = findPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // and returns a Deferred with an ongoing step
            return Deferred<S>(Step.Id.from(newCommand), this)
        } else {
            // else ew return a Deferred linked to pastCommand
            return Deferred<S>(Step.Id.from(pastCommand), this)
        }
    }

    private fun findPastCommandSimilarTo(newCommand: NewCommand): PastCommand? {
        // find pastCommand in current position
        val pastCommand = workflowTaskInput.methodRun.pastCommands
            .find { it.commandPosition == methodRunIndex.methodPosition }

        // if it exists, check it has not changed
        if (pastCommand != null && !pastCommand.isSimilarTo(newCommand, workflowTaskInput.workflowOptions.workflowChangeCheckMode)) {
            throw WorkflowDefinitionUpdatedWhileOngoing(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodRunIndex.methodPosition}"
            )
        }

        return pastCommand
    }

    private fun getPastStepSimilarTo(newStep: NewStep): PastStep? {
        // find pastCommand in current position
        val pastStep = workflowTaskInput.methodRun.pastSteps
            .find { it.stepPosition == methodRunIndex.methodPosition }

        // if it exists, check it has not changed
        if (pastStep != null && !pastStep.isSimilarTo(newStep)) {
            throw WorkflowDefinitionUpdatedWhileOngoing(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodRunIndex.methodPosition}"
            )
        }

        return pastStep
    }
}
