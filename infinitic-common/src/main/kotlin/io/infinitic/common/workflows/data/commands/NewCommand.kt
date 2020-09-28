package io.infinitic.common.workflows.data.commands

import io.infinitic.common.workflows.data.methodRuns.MethodPosition

data class NewCommand(
    val commandId: CommandId = CommandId(),
    val command: Command,
    val commandSimpleName: CommandSimpleName,
    val commandPosition: MethodPosition
) {
    val commandStatus: CommandStatus = CommandStatusOngoing

    val commandHash: CommandHash = command.hash()

    val commandType: CommandType = when (command) {
        is DispatchTask -> CommandType.DISPATCH_TASK
        is DispatchChildWorkflow -> CommandType.DISPATCH_CHILD_WORKFLOW
        is DispatchTimer -> CommandType.DISPATCH_TIMER
        is DispatchReceiver -> CommandType.DISPATCH_RECEIVER
        is StartAsync -> CommandType.START_ASYNC
        is EndAsync -> CommandType.END_ASYNC
        is StartInlineTask -> CommandType.START_INLINE_TASK
        is EndInlineTask -> CommandType.END_INLINE_TASK
    }
}
