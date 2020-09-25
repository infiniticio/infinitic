package io.infinitic.common.workflowManager.data.commands

enum class CommandType {
    DISPATCH_TASK,
    DISPATCH_CHILD_WORKFLOW,
    DISPATCH_TIMER,
    DISPATCH_RECEIVER,
    START_ASYNC,
    END_ASYNC,
    START_INLINE_TASK,
    END_INLINE_TASK
}