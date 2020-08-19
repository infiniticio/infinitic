package com.zenaton.taskManager.common

import com.zenaton.taskManager.common.data.JobAttemptContext

interface Retryable {
    /**
     * Function that returns the delay in seconds to wait before attempting the failed task again.
     * >0: delay in seconds
     * <=0: no delay
     * null: no retry
     */
    fun getRetryDelay(context: JobAttemptContext): Float?
}
