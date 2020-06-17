package com.zenaton.api.task.models

import java.time.Instant

data class TaskAttemptTry(val index: Int, val startedAt: Instant?, val completedAt: Instant?, val failedAt: Instant?, val delayBeforeRetry: Float?) {
    class Builder {
        var index: Int? = null
        var startedAt: Instant? = null
        var completedAt: Instant? = null
        var failedAt: Instant? = null
        var delayBeforeRetry: Float? = null

        fun build(): TaskAttemptTry {
            return TaskAttemptTry(
                index = index ?: throw Exceptions.IncompleteStateException("Index is mandatory to build a TaskAttemptTry object."),
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

fun List<TaskAttemptTry.Builder>.findWithIndex(index: Int): TaskAttemptTry.Builder? = find { it.index == index }
