package io.infinitic.taskManager.tests

import io.infinitic.taskManager.worker.TaskAttemptContext

interface TaskTest {
    fun log()
}

class TaskTestImpl {
    private lateinit var context: TaskAttemptContext
    lateinit var behavior: (index: Int, retry: Int) -> Status

    companion object {
        var log = ""
    }

    fun log() {
        val status = behavior(context.taskAttemptIndex.int, context.taskAttemptRetry.int)

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

    fun getRetryDelay(): Float? = when (behavior(context.taskAttemptIndex.int, context.taskAttemptRetry.int)) {
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
