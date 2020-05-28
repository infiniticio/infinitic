package com.zenaton.taskmanager.messages.interfaces

interface FailingTaskAttemptMessage : TaskAttemptMessage {
    val taskAttemptDelayBeforeRetry: Float?
}
