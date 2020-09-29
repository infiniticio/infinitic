package io.infinitic.common.tasks.messages.interfaces

interface FailingTaskAttemptMessage : TaskAttemptMessage {
    val taskAttemptDelayBeforeRetry: Float?
}
