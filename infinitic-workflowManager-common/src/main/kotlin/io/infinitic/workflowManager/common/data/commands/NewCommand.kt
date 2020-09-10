package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.instructions.Position

data class NewCommand(
    val commandId: CommandId = CommandId(),
    val command: Command,
    val commandSimpleName: CommandSimpleName,
    val commandHash: CommandHash,
    val commandPosition: Position
)
