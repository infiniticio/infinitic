package io.infinitic.workflowManager.common.data.commands

enum class CommandType {
    DISPATCH_TASK,
    DISPATCH_WORKFLOW,
    DISPATCH_TIMER,
    DISPATCH_RECEIVER,
    START_ASYNC,
    END_ASYNC
}
