package io.infinitic.workflowManager.common.data.commands

import io.infinitic.workflowManager.common.data.instructions.StringPosition

data class NewCommand(
    val commandId: CommandId = CommandId(),
    val command: Command,
    val commandSimpleName: CommandSimpleName,
    val commandStringPosition: StringPosition,
    val commandStatus: CommandStatus = CommandStatusOngoing,
    val commandHash: CommandHash = command.hash()
) {
    val commandType: CommandType = when (command) {
        is DispatchTask -> CommandType.DISPATCH_TASK
        is DispatchChildWorkflow -> CommandType.DISPATCH_WORKFLOW
        is DispatchTimer -> CommandType.DISPATCH_TIMER
        is DispatchReceiver -> CommandType.DISPATCH_RECEIVER
        is StartAsync -> CommandType.START_ASYNC
        is EndAsync -> CommandType.END_ASYNC
    }
}
