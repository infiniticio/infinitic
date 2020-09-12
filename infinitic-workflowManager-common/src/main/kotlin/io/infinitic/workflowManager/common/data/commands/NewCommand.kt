package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.instructions.PastPosition

data class NewCommand(
    val commandId: CommandId = CommandId(),
    val command: Command,
    val commandSimpleName: CommandSimpleName,
    val commandHash: CommandHash,
    val commandPastPosition: PastPosition,
    val commandType: CommandType = when(command) {
        is DispatchTask -> CommandType.TASK
        is DispatchChildWorkflow -> CommandType.CHILD_WORKFLOW
        is DispatchTimer -> CommandType.TIMER
        is DispatchReceiver -> CommandType.RECEIVER
    }
)
