package io.infinitic.workflowManager.worker.data

import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.commands.CommandOutput
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.StartAsync
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.methodRuns.MethodPosition
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.methodRuns.MethodOutput
import io.infinitic.workflowManager.common.data.steps.NewStep
import io.infinitic.workflowManager.common.data.steps.PastStep
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.steps.StepStatusCanceled
import io.infinitic.workflowManager.common.data.steps.StepStatusCompleted
import io.infinitic.workflowManager.common.data.steps.StepStatusOngoing
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.workflowManager.common.parser.setPropertiesToObject
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.Workflow
import io.infinitic.workflowManager.worker.deferred.DeferredStatus
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException
import java.lang.RuntimeException
import java.lang.reflect.Method

class MethodRunContext(
    private val workflowTaskInput: WorkflowTaskInput,
    private val workflowInstance: Workflow
) {
    // current position in the tree of method processing
    private var currentMethodPosition: MethodPosition = MethodPosition(null, -1)

    // when method is processed, this value will change each time a step is completed
    private var currentWorkflowTaskIndex: WorkflowTaskIndex = WorkflowTaskIndex(0)

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps (if any) discovered during execution the method
    var newSteps: MutableList<NewStep> = mutableListOf()

    // new workflow properties
    var methodOutput: MethodOutput? = null

    /*
     * Go to next position within the same branch
     */
    fun positionNext() {
        currentMethodPosition = currentMethodPosition.next()
    }

    /*
     * Go to parent branch, this is done at the end of a async { ... } function
     */
    fun positionUp(): Boolean {
        currentMethodPosition.up()?.let { currentMethodPosition = it; return true } ?: return false
    }

    /*
     * Go to child branch, this is done at the start of a async { ... } function
     */
    fun positionDown() {
        currentMethodPosition = currentMethodPosition.down()
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
            taskInput = TaskInput(*args)
        )
        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName.fromMethod(method),
            commandStringPosition = currentMethodPosition.stringPosition
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
     * Async Branch dispatching:
     * we check is this command is already known
     */
    internal fun <S> async(branch: () -> S): Deferred<S> {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartAsync()
        // create instruction that *may* be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("Async"),
            commandStringPosition = currentMethodPosition.stringPosition
        )

        val pastCommand = getPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // run async branch
            positionDown()
            val commandOutput = try {
                CommandOutput(branch())
            } catch(e: Exception) {
                when (e) {
                    is NewStepException -> null
                    is KnownStepException -> null
                    else -> throw e
                }
            }
            positionUp()
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
            stepStringPosition = currentMethodPosition.stringPosition
        )
        val pastStep = getPastStepSimilarTo(newStep)

        // if this is really a new step, we check it directly
        if (pastStep == null) {
            // if this deferred is still ongoing,
            if (newStep.step.stepStatus(currentWorkflowTaskIndex) is StepStatusOngoing) {
                // we add a new step
                newSteps.add(newStep)
                // and stop here
                throw NewStepException()
            }
            // if this deferred is already terminated, we continue
            return deferred
        }

        // check is pastStep is completed
        when (val stepStatus = pastStep.stepStatus) {
            is StepStatusOngoing ->
                throw KnownStepException()
            is StepStatusCompleted ->
                if (currentWorkflowTaskIndex < stepStatus.completionWorkflowTaskIndex) throw KnownStepException()
            is StepStatusCanceled ->
                if (currentWorkflowTaskIndex < stepStatus.cancellationWorkflowTaskIndex) throw KnownStepException()
        }
        // update workflow instance properties
        val properties = pastStep.workflowPropertiesAfterCompletion.mapValues { workflowTaskInput.workflowPropertyStore[it.value] }
        setPropertiesToObject(workflowInstance, properties)
        // continue
        return deferred
    }

    /*
     * Deferred result()
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T> result(deferred: Deferred<T>): T {
        await(deferred)
        // if we are here, then we know that this deferred is completed or canceled
        return when (val status = deferred.step.stepStatus(currentWorkflowTaskIndex)) {
            is StepStatusOngoing -> throw RuntimeException("THIS SHOULD NOT HAPPEN")
            is StepStatusCompleted -> status.result as T
            is StepStatusCanceled -> status.result as T
        }
    }

    /*
     * Deferred status()
     */
    internal fun <T> status(deferred: Deferred<T>): DeferredStatus = when (deferred.step.stepStatus(currentWorkflowTaskIndex)) {
        is StepStatusOngoing -> DeferredStatus.ONGOING
        is StepStatusCompleted -> DeferredStatus.COMPLETED
        is StepStatusCanceled -> DeferredStatus.CANCELED
    }

    private fun getPastCommandSimilarTo(newCommand: NewCommand): PastCommand? {
        // find pastCommand in current position
        val pastCommand = workflowTaskInput.methodRun.pastInstructionsInMethod
            .find { it is PastCommand && it.stringPosition == currentMethodPosition.stringPosition } as PastCommand?

        // if it exists, check it has not changed
        if (pastCommand != null && !pastCommand.isSimilarTo(newCommand, workflowTaskInput.workflowOptions.workflowChangeCheckMode)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${currentMethodPosition.stringPosition}"
            )
        }

        return pastCommand
    }

    private fun getPastStepSimilarTo(newStep: NewStep): PastStep? {
        // find pastCommand in current position
        val pastStep = workflowTaskInput.methodRun.pastInstructionsInMethod
            .find { it is PastStep && it.stringPosition == currentMethodPosition.stringPosition } as PastStep?

        // if it exists, check it has not changed
        if (pastStep != null && !pastStep.isSimilarTo(newStep)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${currentMethodPosition.stringPosition}"
            )
        }

        return pastStep
    }
}
