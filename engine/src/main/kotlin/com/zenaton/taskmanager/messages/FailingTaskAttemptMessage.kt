package com.zenaton.taskmanager.messages

interface FailingTaskAttemptMessage : TaskAttemptMessage {
    val taskAttemptDelayBeforeRetry: Float?
}
