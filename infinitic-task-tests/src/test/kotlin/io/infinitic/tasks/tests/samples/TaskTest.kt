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

package io.infinitic.tasks.tests.samples

import io.infinitic.tasks.executor.task.TaskAttemptContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

interface TaskTest {
    fun log()
}

class TaskException(val log: String) : Exception()

class TaskTestImpl : TaskTest {
    private lateinit var context: TaskAttemptContext
    lateinit var behavior: (index: Int, retry: Int) -> Status
    lateinit var log: String

    override fun log() {
        val status = behavior(context.taskRetry, context.taskAttemptRetry)

        log = context.previousTaskAttemptError?.let { (it as TaskException).log } ?: ""
        log += when (status) {
            Status.SUCCESS -> "1"
            else -> "0"
        }

        when (status) {
            Status.TIMEOUT_WITH_RETRY, Status.TIMEOUT_WITHOUT_RETRY -> runBlocking { delay(1000) }
            Status.FAILED_WITH_RETRY, Status.FAILED_WITHOUT_RETRY -> throw TaskException(log)
            else -> Unit
        }
    }

    fun getRetryDelay(): Float? {
        return when (behavior(context.taskRetry, context.taskAttemptRetry)) {
            Status.FAILED_WITH_RETRY, Status.TIMEOUT_WITH_RETRY -> 0F
            else -> null
        }
    }
}

enum class Status {
    SUCCESS,
    TIMEOUT_WITH_RETRY,
    TIMEOUT_WITHOUT_RETRY,
    FAILED_WITH_RETRY,
    FAILED_WITHOUT_RETRY
}
