package com.zenaton.jobManager.messages.interfaces

interface FailingJobAttemptMessage : JobAttemptMessage {
    val jobAttemptDelayBeforeRetry: Float?
}
