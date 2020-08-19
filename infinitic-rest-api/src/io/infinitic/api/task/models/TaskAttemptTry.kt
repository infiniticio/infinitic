package io.infinitic.api.task.models

import java.time.Instant

data class TaskAttemptTry(val retry: Int, val dispatchedAt: Instant, val startedAt: Instant?, val completedAt: Instant?, val failedAt: Instant?, val delayBeforeRetry: Float?) {
    class Builder {
        var retry: Int? = null
        var dispatchedAt: Instant? = null
        var startedAt: Instant? = null
        var completedAt: Instant? = null
        var failedAt: Instant? = null
        var delayBeforeRetry: Float? = null

        fun build(): TaskAttemptTry {
            return TaskAttemptTry(
                retry = retry ?: throw Exceptions.IncompleteStateException("Retry is mandatory to build a TaskAttemptTry object."),
                dispatchedAt = dispatchedAt ?: throw Exceptions.IncompleteStateException("Dispatched date is mandatory to build a TaskAttemptTry object."),
                startedAt = startedAt,
                completedAt = completedAt,
                failedAt = failedAt,
                delayBeforeRetry = delayBeforeRetry
            )
        }

        object Exceptions {
            class IncompleteStateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
        }
    }
}

/**
 * Transforms a list of `TaskAttemptTry.Builder` to a list of `TaskAttemptTry`.
 */
fun List<TaskAttemptTry.Builder>.build(): List<TaskAttemptTry> = map { it.build() }

fun List<TaskAttemptTry.Builder>.findWithRetry(index: Int): TaskAttemptTry.Builder? = find { it.retry == index }
