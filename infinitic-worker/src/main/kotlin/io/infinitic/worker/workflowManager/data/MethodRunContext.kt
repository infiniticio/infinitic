package io.infinitic.worker.workflowManager.data

import io.infinitic.worker.taskManager.Worker
import io.infinitic.common.workflowManager.data.commands.CommandOutput
import io.infinitic.common.workflowManager.data.commands.CommandSimpleName
import io.infinitic.common.workflowManager.data.commands.CommandStatusCanceled
import io.infinitic.common.workflowManager.data.commands.CommandStatusCompleted
import io.infinitic.common.workflowManager.data.commands.CommandStatusOngoing
import io.infinitic.common.workflowManager.data.commands.CommandType
import io.infinitic.common.workflowManager.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflowManager.data.commands.StartAsync
import io.infinitic.common.workflowManager.data.commands.DispatchTask
import io.infinitic.common.workflowManager.data.commands.EndAsync
import io.infinitic.common.workflowManager.data.commands.EndInlineTask
import io.infinitic.common.workflowManager.data.commands.NewCommand
import io.infinitic.common.workflowManager.data.commands.PastCommand
import io.infinitic.common.workflowManager.data.commands.StartInlineTask
import io.infinitic.common.workflowManager.data.steps.NewStep
import io.infinitic.common.workflowManager.data.steps.PastStep
import io.infinitic.common.workflowManager.data.steps.Step
import io.infinitic.common.workflowManager.data.steps.StepStatusCanceled
import io.infinitic.common.workflowManager.data.steps.StepStatusCompleted
import io.infinitic.common.workflowManager.data.steps.StepStatusOngoing
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.exceptions.ShouldNotWaitInInlineTask
import io.infinitic.common.workflowManager.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.common.workflowManager.parser.setPropertiesToObject
import io.infinitic.worker.workflowManager.deferred.Deferred
import io.infinitic.worker.workflowManager.Workflow
import io.infinitic.worker.workflowManager.deferred.DeferredStatus
import io.infinitic.worker.workflowManager.workflowTasks.KnownStepException
import io.infinitic.worker.workflowManager.workflowTasks.NewStepException
import java.lang.RuntimeException
import java.lang.reflect.Method

class MethodRunContext(
    private val worker: Worker,
    private val workflowTaskInput: WorkflowTaskInput,
    private val workflowInstance: Workflow
) {
    // current position in the tree of method processing
    private var methodLevel: MethodLevel = MethodLevel(messageIndex = workflowTaskInput.methodRun.messageIndexAtStart)

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps (if any) discovered during execution the method
    var newSteps: MutableList<NewStep> = mutableListOf()

    /*
     * Go to next position within the same branch
     */
    private fun positionNext() {
        methodLevel = methodLevel.next()
    }

    /*
     * Go to parent branch, this is done at the end of a async { ... } function
     */
    private fun positionUp() {
        methodLevel.up()?.let { methodLevel = it }
    }

    /*
     * Go to child branch, this is done at the start of a async { ... } function
     */
    private fun positionDown() {
        methodLevel = methodLevel.down()
    }

    /*
     * Command dispatching:
     */
    fun <S> dispatch(method: Method, args: Array<out Any>, returnType: Class<S>): Deferred<S> {
        // increment position
        positionNext()
        // set current command
        val command = if (worker.getInstanceOrNull(method.declaringClass.name) is Workflow)
            DispatchChildWorkflow.from(method, args)
        else
            DispatchTask.from(method, args)

        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = command,
            commandSimpleName = CommandSimpleName.fromMethod(method),
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(newCommand)

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

    /*
     * Deferred await
     */
    internal fun <T> await(deferred: Deferred<T>): Deferred<T> {
        // increment position
        positionNext()
        // create a new step
        val newStep = NewStep(
            step = deferred.step,
            stepPosition = methodLevel.methodPosition
        )
        val pastStep = getPastStepSimilarTo(newStep)

        // if this is really a new step, we check its status based on current workflow message index
        if (pastStep == null) {
            deferred.stepStatus = newStep.step.stepStatusAtMessageIndex(methodLevel.messageIndex)
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
        methodLevel.messageIndex = when (val stepStatus = deferred.stepStatus) {
            is StepStatusOngoing -> throw KnownStepException()
            is StepStatusCompleted -> stepStatus.completionWorkflowMessageIndex
            is StepStatusCanceled -> stepStatus.cancellationWorkflowMessageIndex
        }

        // update workflow instance properties
        val properties = pastStep.propertiesAtTermination!!.mapValues { workflowTaskInput.workflowPropertyStore[it.value] }
        setPropertiesToObject(workflowInstance, properties)

        // continue
        return deferred
    }

    /*
     * Async Branch dispatching:
     */
    internal fun <S> async(branch: () -> S): Deferred<S> {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartAsync
        // create instruction that *may* be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("${CommandType.START_ASYNC}"),
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // run async branch
            runAsync(branch)

            // returns a Deferred with an ongoing step
            return Deferred<S>(Step.Id.from(newCommand), this)
        } else {
            // branch is processed only if not yet completed or canceled
            if (pastCommand.commandStatus is CommandStatusOngoing) {
                runAsync(branch)
            }

            // returns a Deferred linked to pastCommand
            return Deferred<S>(Step.Id.from(pastCommand), this)
        }
    }

    /*
     * When running an async branch:
     * - unknown step breaks only current branch
     * - output is returned through a special EndAsync command
     */
    private fun <S> runAsync(branch: () -> S) {
        // go down
        positionDown()

        val commandOutput = try {
            CommandOutput(branch())
        } catch (e: Exception) {
            when (e) {
                is NewStepException -> null
                is KnownStepException -> null
                else -> throw e
            }
        }
        // go up - note that message index comes back to its previous value before going down
        positionUp()

        // if the branch is completed, then we send a special command to the engine
        if (commandOutput != null) {
            // create instruction that *may* be sent to engine
            val newCommand = NewCommand(
                command = EndAsync(commandOutput),
                commandSimpleName = CommandSimpleName("${CommandType.END_ASYNC}"),
                commandPosition = methodLevel.methodPosition
            )
            newCommands.add(newCommand)
        }
    }

    /*
     * Inline task
     */
    internal fun <S> task(inline: () -> S): S {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartInlineTask
        // create instruction that *may* be sent to engine
        val startCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("${CommandType.START_INLINE_TASK}"),
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(startCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(startCommand)
            // go down (it should be needed only if inline task dispatch some tasks)
            positionDown()
            // run inline task
            val commandOutput = try {
                CommandOutput(inline())
            } catch (e: Exception) {
                when (e) {
                    is NewStepException -> throw ShouldNotWaitInInlineTask()
                    is KnownStepException -> throw ShouldNotWaitInInlineTask()
                    else -> throw e
                }
            }
            // go up
            positionUp()
            // record result
            val endCommand = NewCommand(
                command = EndInlineTask(commandOutput),
                commandSimpleName = CommandSimpleName("${CommandType.END_INLINE_TASK}"),
                commandPosition = methodLevel.methodPosition
            )
            newCommands.add(endCommand)
            // returns a Deferred with an ongoing step
            return commandOutput.data as S
        } else {
            return when (val status = pastCommand.commandStatus) {
                is CommandStatusOngoing -> throw RuntimeException("This should not happen: uncompleted inline task")
                is CommandStatusCanceled -> throw RuntimeException("This should not happen: canceled inline task")
                is CommandStatusCompleted -> status.completionResult.data as S
            }
        }
    }

    /*
     * Deferred result()
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T> result(deferred: Deferred<T>): T = when (val status = await(deferred).stepStatus) {
        is StepStatusOngoing -> throw RuntimeException("THIS SHOULD NOT HAPPEN: asking result of an ongoing deferred")
        is StepStatusCompleted -> status.completionResult.data as T
        is StepStatusCanceled -> status.cancellationResult.data as T
    }

    /*
     * Deferred status()
     */
    internal fun <T> status(deferred: Deferred<T>): DeferredStatus = when (deferred.step.stepStatusAtMessageIndex(methodLevel.messageIndex)) {
        is StepStatusOngoing -> DeferredStatus.ONGOING
        is StepStatusCompleted -> DeferredStatus.COMPLETED
        is StepStatusCanceled -> DeferredStatus.CANCELED
    }

    private fun getPastCommandSimilarTo(newCommand: NewCommand): PastCommand? {
        // find pastCommand in current position
        val pastCommand = workflowTaskInput.methodRun.pastCommands
            .find { it.commandPosition == methodLevel.methodPosition }

        // if it exists, check it has not changed
        if (pastCommand != null && !pastCommand.isSimilarTo(newCommand, workflowTaskInput.workflowOptions.workflowChangeCheckMode)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodLevel.methodPosition}"
            )
        }

        return pastCommand
    }

    private fun getPastStepSimilarTo(newStep: NewStep): PastStep? {
        // find pastCommand in current position
        val pastStep = workflowTaskInput.methodRun.pastSteps
            .find { it.stepPosition == methodLevel.methodPosition }

        // if it exists, check it has not changed
        if (pastStep != null && !pastStep.isSimilarTo(newStep)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodLevel.methodPosition}"
            )
        }

        return pastStep
    }
}
