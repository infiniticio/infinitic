package io.infinitic.workflowManager.worker.data

import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.methods.MethodPosition
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.instructions.PastStep
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.worker.Deferred.Deferred
import io.infinitic.workflowManager.worker.Workflow
import java.lang.reflect.Method

class MethodContext(
    private val workflowTaskInput: WorkflowTaskInput,
    private val workflowInstance: Workflow
) {
    // current position in the tree of method processing
    private var currentMethodPosition: MethodPosition = MethodPosition(null, -1)

    // when method is processed, this value will change each time a step is completed
    private var currentWorkflowTaskIndex : WorkflowTaskIndex = WorkflowTaskIndex(0)

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new step (if any) discovered during execution the method
    var newStep: Step? = null

    /*
     * Go to next position within the same branch
     */
    fun next() {
        currentMethodPosition = currentMethodPosition.next()
    }

    /*
     * Go to parent branch, this is done at the end of a async { ... } function
     */
    fun up(): Boolean {
        currentMethodPosition.up()?.let { currentMethodPosition = it; return true } ?: return false
    }

    /*
     * Go to child branch, this is done at the start of a async { ... } function
     */
    fun down() {
        currentMethodPosition = currentMethodPosition.down()
    }

    /*
     * Command dispatching:
     * we check is this command is already known
     *
     */
    fun <S> dispatch(method: Method, args: Array<out Any>, type: Class<S>): Deferred<S> {
        // increment current command index
        next()
        // set current command
        val dispatch = DispatchTask(
            taskName = TaskName.from(method),
            taskInput = TaskInput(*args)
        )
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName.fromMethod(method),
            commandHash = dispatch.hash(),
            commandPastPosition = currentMethodPosition.pastPosition
        )

        val pastCommand = getPastInstructionSimilarTo(newCommand) as PastCommand?

        // if this is a new command, we add it to the newCommands list
        if (pastCommand == null) {
            newCommands.add(newCommand)
        }

        val commandId  = pastCommand?.commandId ?: newCommand.commandId

        return Deferred<S>(
            Step.Id(commandId) { stepIdStatus(commandId) }
        )
//            id = "${pastCommand?.commandId ?: newCommand.commandId}",
//            newCommand = newCommand,
//            pastCommand = pastCommand,
//            methodContext = this
//        )
    }

    private fun <S> await(step: Step) {
        val pastStep = getPastStepSimilarTo(step)

        // if this is a new step, we check it directly
        if (pastStep == null) {
            // check is step is completed

            // if yes continue and return Deferred
            // if no, add step as newStep and throw NewStep
        }

        // check is pastStep is completed

        // if yes continue and return Deferred
        // if no, add step as newStep and throw NewStep
    }


    private fun <S> result(step: Step) {

    }

    private fun stepIdStatus(commandId: CommandId) : Step.Status {
        val pastCommand = workflowTaskInput.method.methodPastInstructions
            .filterIsInstance<PastCommand>()
            .find { it.commandId == commandId }

        if (pastCommand == null) return Step.Status.ONGOING

        if (pastCommand.commandStatus == CommandStatus.COMPLETED
    }

    private fun getPastInstructionSimilarTo(newCommand: NewCommand): PastInstruction? {
        // find pastCommand in current position
        val pastInstruction = workflowTaskInput.method.methodPastInstructions.find { it.pastPosition == currentMethodPosition.pastPosition }

        // if it exists, check it has not changed
        if (pastInstruction != null && !pastInstruction.isSimilarTo(newCommand, workflowTaskInput.workflowOptions.workflowChangeCheckMode)) {
            throw WorkflowUpdatedWhileRunning(workflowTaskInput.workflowName.name, "${newCommand.commandSimpleName}", "${currentMethodPosition.pastPosition}")
        }

        return pastInstruction
    }

    private fun getPastStepSimilarTo(step: Step): PastStep? {

    }
