package com.zenaton.taskManager.common.messages.interfaces

interface FailingTaskAttemptMessage : TaskAttemptMessage {
    val taskAttemptDelayBeforeRetry: Float?
}
