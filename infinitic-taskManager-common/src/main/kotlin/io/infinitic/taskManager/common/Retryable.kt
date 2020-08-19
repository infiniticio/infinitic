package io.infinitic.taskManager.common

import io.infinitic.taskManager.common.data.TaskAttemptContext

interface Retryable {
    /**
     * Function that returns the delay in seconds to wait before attempting the failed task again.
     * >0: delay in seconds
     * <=0: no delay
     * null: no retry
     */
    fun getRetryDelay(context: TaskAttemptContext): Float?
}
