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
import org.jetbrains.annotations.TestOnly

@Suppress("unused")
object Task {

  @JvmStatic
  fun getContext(taskId: String): TaskContext? = threadLocalBatchContext.get()[taskId]

  /**
   * This method is intended for test purposes only. It allows you to manually assign a `TaskContext`
   * to the current thread, which can be useful for simulating task execution in unit tests.
   *
   * @param context The `TaskContext` to set for the current thread.
   */
  @TestOnly
  @JvmStatic
  fun setContext(context: TaskContext) {
    threadLocalTaskContext.set(context)
  }

  /**
   * This method is intended for test purposes only. It allows you to manually assign a
   * map of task IDs to their corresponding `TaskContext` to the current thread,
   * which can be useful for simulating batch execution in unit tests.
   *
   * @param context A map where the key is the task ID and the value is the corresponding `TaskContext`.
   */
  @TestOnly
  @JvmStatic
  fun setBatchContext(context: Map<String, TaskContext>) {
    threadLocalBatchContext.set(context)
  }

  /**
   * This method is intended for test purposes only. It allows you to manually assign a hasTimedOut
   * status for the current task, which can be useful for simulating task execution in unit tests.
   *
   * @param hasTimedOut `true` if the task has timed out, `false` otherwise.
   */
  @TestOnly
  @JvmStatic
  fun setHasTimedOut(hasTimedOut: Boolean) {
    TimeoutContext.current()._isCancelled.set(hasTimedOut)
  }

  /**
   * The current [TaskContext]
   */
  @JvmStatic
  val context: TaskContext?
    get() = threadLocalTaskContext.get()

  /**
   * The name of the worker
   */
  @JvmStatic
  val workerName
    get() = threadLocalTaskContext.get().workerName

  /**
   * The name of the Service that the task belongs to.
   */
  @JvmStatic
  val serviceName: String
    get() = threadLocalTaskContext.get().serviceName.toString()

  /**
   * The unique identifier for the task.
   */
  @JvmStatic
  val taskId: String
    get() = threadLocalTaskContext.get().taskId.toString()

  /**
   * The name of the task being executed.
   */
  @JvmStatic
  val taskName: String
    get() = threadLocalTaskContext.get().taskName.toString()

  /**
   * The unique identifier for the workflow associated with the task.
   */
  @JvmStatic
  val workflowId: String?
    get() = threadLocalTaskContext.get().workflowId?.toString()

  /**
   * The name of the workflow associated with the task.
   */
  @JvmStatic
  val workflowName: String?
    get() = threadLocalTaskContext.get().workflowName?.toString()

  /**
   * The version of the workflow associated with the task.
   */
  @JvmStatic
  val workflowVersion: Int?
    get() = threadLocalTaskContext.get().workflowVersion?.toInt()

  /**
   * The last error encountered during task execution.
   */
  @JvmStatic
  val lastError: TaskFailure?
    get() = threadLocalTaskContext.get().lastError

  /**
   * The sequence number of the current retry attempt for the task.
   */
  @JvmStatic
  val retrySequence: Int
    get() = threadLocalTaskContext.get().retrySequence.toInt()

  /**
   * The index of the current retry attempt for the task.
   */
  @JvmStatic
  val retryIndex: Int
    get() = threadLocalTaskContext.get().retryIndex.toInt()

  /**
   * The batch key associated with the task, if it is part of a batch.
   */
  @JvmStatic
  val batchKey: String?
    get() = threadLocalTaskContext.get().batchKey

  /**
   * A set of tags associated with the task.
   */
  @JvmStatic
  val tags: Set<String>
    get() = threadLocalTaskContext.get().tags

  /**
   * A mutable map of metadata associated with the task.
   */
  @JvmStatic
  val meta: MutableMap<String, ByteArray>
    get() = threadLocalTaskContext.get().meta

  /**
   * The timeout configuration for the task, if defined.
   */
  @JvmStatic
  val withTimeout: WithTimeout?
    get() = threadLocalTaskContext.get().withTimeout

  /**
   * The retry configuration for the task, if defined.
   */
  @JvmStatic
  val withRetry: WithRetry?
    get() = threadLocalTaskContext.get().withRetry

  /**
   * The Infinitic client used to interact with the Infinitic system.
   */
  @JvmStatic
  val client: InfiniticClientInterface
    get() = threadLocalTaskContext.get().client

  /**
   * Indicates whether the current task execution has timed out.
   */
  @JvmStatic
  val hasTimedOut: Boolean
    get() = TimeoutContext.current().isCancelled

  /**
   * Sets a callback function to be executed when the task times out.
   */
  @JvmStatic
  fun onTimeOut(callback: () -> Unit) {
    TimeoutContext.current().onTimeout(callback)
  }
}

val threadLocalTaskContext: ThreadLocal<TaskContext> =
    ThreadLocal.withInitial { null }

val threadLocalBatchContext: ThreadLocal<Map<String, TaskContext>> =
    ThreadLocal.withInitial { mapOf() }

