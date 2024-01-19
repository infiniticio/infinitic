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

package io.infinitic.events

import io.infinitic.common.data.MillisDuration
import io.infinitic.events.data.Data
import io.infinitic.events.errors.Error
import io.infinitic.events.methods.Method
import kotlinx.serialization.Serializable

/**
 * Represents a log entry for a task.
 *
 * @property serviceName is the name of the requested service
 * @property taskId is the id of the task
 * @property retrySequence is initially 0 and is incremented by a manual retry
 * @property retryIndex is initially 0 and is incremented by an automatic retry, reset to 0 by a manual retry
 * [taskId], [retrySequence] & [retryIndex] uniquely identify a task execution attempt
 *
 */
@Serializable
sealed interface TaskEvent : Event {
  val serviceName: String
  val taskId: String
  val retrySequence: Int
  val retryIndex: Int

  companion object {
    const val TYPE_PREFIX = "io.infinitic.task"
  }
}

/**
 * Represents a dispatched task log entry.
 *
 * @property method The method call associated with the task.
 * @property meta The meta information of the task.
 * @property tags The tags associated with the task.
 */
@Serializable
data class TaskDispatchedData(
  val method: Method,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
)

@Serializable
data class TaskFailedData(
  val error: Error,
)

@Serializable
data class TaskRetriedData(
  val delayMillis: MillisDuration,
  val error: Error,
)

@Serializable
data class TaskCompletedData(
  val result: Data
)
