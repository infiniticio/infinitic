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

package io.infinitic.tests.tasks

import io.infinitic.clients.cancelTask
import io.infinitic.clients.cancelWorkflow
import io.infinitic.clients.retryTask
import io.infinitic.tasks.Task
import io.infinitic.tests.workflows.WorkflowA
import java.time.Duration
import java.util.UUID

interface TaskA {
    fun concat(str1: String, str2: String): String
    fun reverse(str: String): String
    fun await(delay: Long): Long
    fun workflowId(): UUID?
    fun workflowName(): String?
    fun cancelWorkflowA(id: UUID)
    fun cancelTaskA(id: UUID)
    fun failing()
    fun successAtRetry(): String
    fun retryTaskA(id: UUID)
}

class TaskAImpl : Task(), TaskA {
    override fun concat(str1: String, str2: String) = str1 + str2
    override fun reverse(str: String) = str.reversed()
    override fun await(delay: Long): Long { Thread.sleep(delay); return delay }
    override fun workflowId() = context.workflowId
    override fun workflowName() = context.workflowName
    override fun cancelWorkflowA(id: UUID) {
        Thread.sleep(50)
        context.client.cancelWorkflow<WorkflowA>(id)
    }

    override fun cancelTaskA(id: UUID) {
        Thread.sleep(50)
        context.client.cancelTask<TaskA>(id)
    }

    override fun failing() = throw Exception("sorry")
    override fun successAtRetry() = when (context.retrySequence) {
        0 -> throw Exception()
        else -> "ok"
    }
    override fun retryTaskA(id: UUID) {
        Thread.sleep(50)
        context.client.retryTask<TaskA>(id)
    }

    override fun getDurationBeforeRetry(e: Exception): Duration? = null
}
