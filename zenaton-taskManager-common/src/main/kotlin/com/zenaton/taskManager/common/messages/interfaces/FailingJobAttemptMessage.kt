package com.zenaton.taskManager.common.messages.interfaces

interface FailingJobAttemptMessage : JobAttemptMessage {
    val jobAttemptDelayBeforeRetry: Float?
}
