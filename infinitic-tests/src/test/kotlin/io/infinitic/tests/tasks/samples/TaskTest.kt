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

package io.infinitic.tests.tasks.samples

import io.infinitic.common.tasks.Task
import io.infinitic.worker.task.TaskAttemptContext

interface TaskTest : Task {
    fun log()
}

class TaskTestImpl : TaskTest {
    private lateinit var context: TaskAttemptContext
    lateinit var behavior: (index: Int, retry: Int) -> Status

    companion object {
        var log = ""
    }

    override fun log() {
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
