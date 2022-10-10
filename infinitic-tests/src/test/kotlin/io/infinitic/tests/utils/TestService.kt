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

package io.infinitic.tests.utils

import io.infinitic.tasks.Retryable
import io.infinitic.tasks.Task

interface TestService {
    fun log(): String
    fun await(delay: Long): Long
}

class ExpectedException(log: String? = null) : Exception(log)

@Suppress("unused")
class TestServiceImpl : TestService, Retryable {
    companion object {
        lateinit var behavior: (index: Int, retry: Int) -> Status
    }

    var log = ""

    override fun log(): String {
        Thread.sleep(50)

        val status = behavior(Task.retrySequence, Task.retryIndex)

        log = Task.meta["log"]?.let { String(it) } ?: ""
        log += when (status) {
            Status.SUCCESS -> "1"
            else -> "0"
        }
        Task.meta["log"] = log.encodeToByteArray()

        when (status) {
            Status.TIMEOUT_WITH_RETRY, Status.TIMEOUT_WITHOUT_RETRY -> Thread.sleep(1000)
            Status.FAILED_WITH_RETRY, Status.FAILED_WITHOUT_RETRY -> throw ExpectedException(log)
            else -> Unit
        }

        return log
    }

    override fun await(delay: Long): Long {
        Thread.sleep(delay)

        return delay
    }

    override fun getSecondsBeforeRetry(attempt: Int, exception: Exception): Double? =
        when (behavior(Task.retrySequence, Task.retryIndex)) {
            Status.FAILED_WITH_RETRY, Status.TIMEOUT_WITH_RETRY -> 10.0
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
