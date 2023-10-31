/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.tests.utils

import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.tasks.Task
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workflows.DeferredStatus

interface ParentInterface {
  fun parent(): String
}

interface UtilService : ParentInterface {
  fun concat(str1: String, str2: String): String

  fun reverse(str: String): String

  fun await(delay: Long): Long

  fun workflowId(): String?

  fun workflowName(): String?

  fun retryFailedTasks(workflowName: String, id: String)

  fun cancelWorkflow(workflowName: String, id: String)

  fun failing()

  fun successAtRetry(): String

  fun tags(): Set<String>

  fun meta(): TaskMeta

  fun withRetry(): WithRetry?

  fun withTimeout(): WithTimeout?
}

@Suppress("unused")
class UtilServiceImpl : UtilService {
  override fun concat(str1: String, str2: String): String = str1 + str2

  override fun reverse(str: String) = str.reversed()

  override fun await(delay: Long): Long {
    Thread.sleep(delay)
    return delay
  }

  override fun workflowId() = Task.workflowId

  override fun workflowName() = Task.workflowName

  override fun retryFailedTasks(workflowName: String, id: String) {
    Thread.sleep(50)
    val w = Task.client.getWorkflowById(Class.forName(workflowName), id)
    Task.client.retryTasks(w, taskStatus = DeferredStatus.FAILED)
  }

  override fun cancelWorkflow(workflowName: String, id: String) {
    Thread.sleep(50)
    val w = Task.client.getWorkflowById(Class.forName(workflowName), id)
    Task.client.cancel(w)
  }

  override fun failing() = throw Exception("sorry")

  override fun successAtRetry() =
      when (Task.retrySequence) {
        0 -> throw ExpectedException()
        else -> "ok"
      }

  override fun parent() = "ok"

  override fun tags() = Task.tags

  override fun meta() = TaskMeta(Task.meta)

  override fun withRetry(): WithRetry? = Task.context.get().withRetry

  override fun withTimeout(): WithTimeout? = Task.context.get().withTimeout
}
