package com.zenaton.taskManager.messages

interface FailingJobAttemptMessage : JobAttemptMessage {
    val jobAttemptDelayBeforeRetry: Float?
}
