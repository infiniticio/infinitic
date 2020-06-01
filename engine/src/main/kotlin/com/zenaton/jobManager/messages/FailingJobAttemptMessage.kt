package com.zenaton.jobManager.messages

interface FailingJobAttemptMessage : JobAttemptMessage {
    val jobAttemptDelayBeforeRetry: Float?
}
