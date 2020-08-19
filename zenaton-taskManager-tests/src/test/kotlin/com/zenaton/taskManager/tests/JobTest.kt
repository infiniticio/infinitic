package com.zenaton.taskManager.tests

import com.zenaton.taskManager.common.data.JobAttemptContext
import com.zenaton.taskManager.worker.Worker

interface JobTest {
    fun log()
}

class JobTestImpl {
    lateinit var behavior: (index: Int, retry: Int) -> Status

    companion object {
        var log = ""
    }

    fun log() {
        val context = Worker.context
        val status = behavior(context.jobAttemptIndex.int, context.jobAttemptRetry.int)

        log += when (status) {
            Status.SUCCESS -> "1"
            else -> "0"
        }

        when (status) {
            Status.TIMEOUT_WITH_RETRY, Status.TIMEOUT_WITHOUT_RETRY -> Thread.sleep(1000)
            Status.FAILED_WITH_RETRY, Status.FAILED_WITHOUT_RETRY -> throw Exception()
            else -> Unit
        }
    }

    fun getRetryDelay(context: JobAttemptContext): Float? = when (behavior(context.jobAttemptIndex.int, context.jobAttemptRetry.int)) {
        Status.FAILED_WITH_RETRY, Status.TIMEOUT_WITH_RETRY -> 0F
        else -> null
    }
}

enum class Status {
    SUCCESS,
    TIMEOUT_WITH_RETRY,
    TIMEOUT_WITHOUT_RETRY,
    FAILED_WITH_RETRY,
    FAILED_WITHOUT_RETRY
}
