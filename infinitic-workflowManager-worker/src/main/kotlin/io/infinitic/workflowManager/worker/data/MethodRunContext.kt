package io.infinitic.workflowManager.worker.data

import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.commands.CommandOutput
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.commands.StartAsync
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.commands.EndAsync
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.steps.NewStep
import io.infinitic.workflowManager.common.data.steps.PastStep
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.steps.StepStatusCanceled
import io.infinitic.workflowManager.common.data.steps.StepStatusCompleted
import io.infinitic.workflowManager.common.data.steps.StepStatusOngoing
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.Workflow
import io.infinitic.workflowManager.worker.deferred.DeferredStatus
import io.infinitic.workflowManager.worker.workflowTasks.KnownStepException
import io.infinitic.workflowManager.worker.workflowTasks.NewStepException
import java.lang.RuntimeException
import java.lang.reflect.Method

class MethodRunContext(
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
     * we check is this command is already known
     */
    fun <S> dispatch(method: Method, args: Array<out Any>, type: Class<S>): Deferred<S> {
        // increment position
        positionNext()
        // set current command
        val dispatch = DispatchTask(
            taskName = TaskName.from(method),
            taskInput = TaskInput.from(method, args),
            taskMeta = TaskMeta().withParametersTypesFrom(method)
        )

        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
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
     * we check is this command is already known
     */
    internal fun <S> async(branch: () -> S): Deferred<S> {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartAsync
        // create instruction that *may* be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("StartAsync"),
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
                commandSimpleName = CommandSimpleName("EndAsync"),
                commandPosition = methodLevel.methodPosition
            )
            newCommands.add(newCommand)
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
