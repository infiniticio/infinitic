package io.infinitic.workflowManager.common.data.methodRuns

import io.infinitic.common.data.interfaces.plus
import io.infinitic.workflowManager.common.data.instructions.StringPosition
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

data class MethodPosition(
    val parentPosition: MethodPosition? = null,
    val instructionIndex: MethodInstructionIndex = MethodInstructionIndex(-1),
    var messageIndex: WorkflowMessageIndex
) {
    val stringPosition: StringPosition = when (parentPosition) {
        null -> StringPosition("$instructionIndex")
        else -> StringPosition("${parentPosition.stringPosition}.$instructionIndex")
    }

    override fun toString() = "$stringPosition"

    fun next() = MethodPosition(parentPosition, instructionIndex + 1, messageIndex)

    fun up() = parentPosition

    fun down() = MethodPosition(this, MethodInstructionIndex(-1), messageIndex)
}
