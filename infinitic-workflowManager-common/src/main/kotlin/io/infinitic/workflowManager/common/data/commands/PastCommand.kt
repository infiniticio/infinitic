package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.instructions.StringPosition
import io.infinitic.workflowManager.common.data.workflows.WorkflowChangeCheckMode

data class PastCommand(
    override val stringPosition: StringPosition,
    val commandType: CommandType,
    val commandId: CommandId,
    val commandHash: CommandHash,
    val commandSimpleName: CommandSimpleName,
    val commandStatus: CommandStatus
) : PastInstruction(stringPosition) {
    fun isSimilarTo(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean =
        newCommand.commandStringPosition == stringPosition &&
            when (mode) {
                WorkflowChangeCheckMode.NONE ->
                    true
                WorkflowChangeCheckMode.SIMPLE_NAME_ONLY ->
                    newCommand.commandType == commandType && newCommand.commandSimpleName == commandSimpleName
                WorkflowChangeCheckMode.ALL ->
                    newCommand.commandHash == commandHash
            }
}
