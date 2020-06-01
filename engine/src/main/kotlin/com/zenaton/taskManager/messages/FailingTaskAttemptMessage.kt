package com.zenaton.taskManager.messages

interface FailingTaskAttemptMessage : TaskAttemptMessage {
    val taskAttemptDelayBeforeRetry: Float?
}
