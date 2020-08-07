package com.zenaton.jobManager.common

import com.zenaton.jobManager.common.data.JobAttemptContext

interface Retryable {
    fun delayBeforeRetry(context: JobAttemptContext): Float?
}
