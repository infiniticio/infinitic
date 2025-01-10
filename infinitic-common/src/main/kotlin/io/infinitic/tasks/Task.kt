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
package io.infinitic.tasks

import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.tasks.executors.errors.TaskFailure
import org.jetbrains.annotations.TestOnly

object Task {
  private val context: ThreadLocal<TaskContext> = ThreadLocal.withInitial { null }

  private val _batchContext: ThreadLocal<Map<String, TaskContext>> =
      ThreadLocal.withInitial { mapOf() }

  @JvmStatic
  fun getContext(taskId: String): TaskContext? = _batchContext.get()[taskId]

  @TestOnly
  @JvmStatic
  fun setContext(taskId: String, taskContext: TaskContext) {
    val batchContext = _batchContext.get().toMutableMap()
    batchContext[taskId] = taskContext
    _batchContext.set(batchContext)
  }

  @JvmStatic
  fun getContext(): TaskContext? = context.get()

  @TestOnly
  @JvmStatic
  fun setContext(c: TaskContext) {
    context.set(c)
  }

  @JvmStatic
  val workerName
    get() = context.get().workerName

  @JvmStatic
  val serviceName: String
    get() = context.get().serviceName.toString()

  @JvmStatic
  val taskId: String
    get() = context.get().taskId.toString()

  @JvmStatic
  val taskName: String
    get() = context.get().taskName.toString()

  @JvmStatic
  val workflowId: String?
    get() = context.get().workflowId?.toString()

  @JvmStatic
  val workflowName: String?
    get() = context.get().workflowName?.toString()

  @JvmStatic
  val workflowVersion: Int?
    get() = context.get().workflowVersion?.toInt()

  @JvmStatic
  val lastError: TaskFailure?
    get() = context.get().lastError

  @JvmStatic
  val retrySequence: Int
    get() = context.get().retrySequence.toInt()

  @JvmStatic
  val retryIndex: Int
    get() = context.get().retryIndex.toInt()

  @JvmStatic
  val batchKey: String?
    get() = context.get().batchKey

  @JvmStatic
  val tags: Set<String>
    get() = context.get().tags

  @JvmStatic
  val meta: MutableMap<String, ByteArray>
    get() = context.get().meta

  @JvmStatic
  val withTimeout: WithTimeout?
    get() = context.get().withTimeout

  @JvmStatic
  val withRetry: WithRetry?
    get() = context.get().withRetry

  @JvmStatic
  val client: InfiniticClientInterface
    get() = context.get().client
}
