package com.zenaton.jobManager.common.messages.interfaces

interface FailingJobAttemptMessage : JobAttemptMessage {
    val jobAttemptDelayBeforeRetry: Float?
}
