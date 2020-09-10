package io.infinitic.workflowManager.worker.data

import io.infinitic.workflowManager.common.data.methods.MethodPosition
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.common.exceptions.WorkflowUpdatedWhileRunning

data class MethodContext(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val workflowOptions: WorkflowOptions,
    val pastInstructions: List<PastInstruction>,
    var currentMethodPosition: MethodPosition = MethodPosition(null, -1),
    var newStep: Step? = null,
    var newCommands: MutableList<NewCommand> = mutableListOf()
) {
    fun getPastInstructionSimilarTo(newCommand: NewCommand): PastInstruction? {
        // find pastCommand in current position
        val pastInstruction = pastInstructions.find { it.position == currentMethodPosition.position }

        // if it exists, check it has not changed
        if (pastInstruction != null && !pastInstruction.isSimilarTo(newCommand, workflowOptions.workflowIntegrityCheckMode)) {
            throw WorkflowUpdatedWhileRunning(workflowName.name, "${newCommand.commandSimpleName}", "${currentMethodPosition.position}")
        }

        return pastInstruction
    }

    fun next() {
        currentMethodPosition = currentMethodPosition.next()
    }

    fun up(): Boolean {
        currentMethodPosition.up()?.let { currentMethodPosition = it; return true } ?: return false
    }

    fun down() {
        currentMethodPosition = currentMethodPosition.down()
    }
}
