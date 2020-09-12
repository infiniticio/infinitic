package io.infinitic.workflowManager.common.data.instructions

import io.infinitic.workflowManager.common.data.commands.CommandHash
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandOutput
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.CommandType
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowChangeCheckMode
import io.infinitic.workflowManager.data.commands.CommandStatus

data class PastCommand(
    override val pastPosition: PastPosition,
    val commandType: CommandType,
    val commandId: CommandId,
    val commandHash: CommandHash,
    val commandSimpleName: CommandSimpleName,
    val commandStatus: CommandStatus
) : PastInstruction(pastPosition) {
    override fun isSimilarTo(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean =
        newCommand.commandPastPosition == pastPosition &&
            when (mode) {
                WorkflowChangeCheckMode.NONE ->
                    true
                WorkflowChangeCheckMode.SIMPLE_NAME_ONLY ->
                    newCommand.commandType == commandType && newCommand.commandSimpleName == commandSimpleName
                WorkflowChangeCheckMode.ALL ->
                    newCommand.commandHash == commandHash
            }
}

