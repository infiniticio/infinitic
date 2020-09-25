package io.infinitic.workflowManager.worker.data

import io.infinitic.common.workflowManager.data.methodRuns.MethodPosition
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex

data class MethodLevel(
    val parentLevel: MethodLevel? = null,
    val instructionIndex: Int = -1,
    var messageIndex: WorkflowMessageIndex
) {
    val methodPosition: MethodPosition = when (parentLevel) {
        null -> MethodPosition("$instructionIndex")
        else -> MethodPosition("${parentLevel.methodPosition}.$instructionIndex")
    }

    override fun toString() = "$methodPosition"

    fun next() = MethodLevel(parentLevel, instructionIndex + 1, messageIndex)

    fun up() = parentLevel

    fun down() = MethodLevel(this, -1, messageIndex)
}
