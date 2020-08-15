package com.zenaton.jobManager.tests

import com.zenaton.jobManager.common.data.JobAttemptContext
import com.zenaton.jobManager.worker.Worker

interface JobTest {
    fun log(i: Int): String
}

class JobTestImpl {
    lateinit var behavior: (index: Int, retry: Int) -> Status

    companion object {
        var log = ""
    }

    fun log(i: Int): String {
        log += i

        val context = Worker.getContext()
        when (behavior(context.jobAttemptIndex.int, context.jobAttemptRetry.int)) {
            Status.SUCCESS -> log += "END"
            Status.TIMEOUT_WITH_RETRY, Status.TIMEOUT_WITHOUT_RETRY -> Thread.sleep(1000)
            Status.FAILED_WITH_RETRY, Status.FAILED_WITHOUT_RETRY -> throw Exception()
        }

        return i.toString()
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
