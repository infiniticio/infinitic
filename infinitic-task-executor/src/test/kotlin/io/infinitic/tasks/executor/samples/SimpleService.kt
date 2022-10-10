/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

@file:Suppress("unused")

package io.infinitic.tasks.executor.samples

import io.infinitic.annotations.Retry
import io.infinitic.exceptions.tasks.MaxRunDurationException
import io.infinitic.tasks.Retryable
import io.infinitic.tasks.Task

interface SimpleService {
    fun handle(i: Int, j: String): String
    fun handle(i: Int, j: Int): Int
    fun other(i: Int, j: String): String
}

class ServiceImplService : SimpleService {
    override fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    override fun handle(i: Int, j: Int) = (i * j)
    override fun other(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class SimpleServiceWithContext {
    fun handle(i: Int, j: String) = (i * j.toInt() * Task.retrySequence).toString()
}

internal class SimpleServiceWithRetry {
    @Retry(RetryImpl::class)
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
}

@Retry(BuggyRetryImpl::class)
internal class SimpleServiceWithBuggyRetry {
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
}

internal class SimpleServiceWithTimeout : Retryable {
    fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * Task.retrySequence).toString()
    }

    override fun getSecondsBeforeRetry(attempt: Int, exception: Exception) =
        if (exception is MaxRunDurationException) null else 3.0
}

internal class RetryImpl : Retryable {
    override fun getSecondsBeforeRetry(attempt: Int, exception: Exception) =
        if (exception is IllegalStateException) 3.0 else null
}

internal class BuggyRetryImpl : Retryable {
    override fun getSecondsBeforeRetry(attempt: Int, exception: Exception) =
        if (exception is IllegalStateException) throw IllegalArgumentException() else 3.0
}
