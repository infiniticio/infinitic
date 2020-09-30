// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

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
