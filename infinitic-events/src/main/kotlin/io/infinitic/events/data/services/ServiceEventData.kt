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

package io.infinitic.events.data.services

import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.events.TaskCompleted
import io.infinitic.events.TaskFailed
import io.infinitic.events.TaskRetried
import io.infinitic.events.TaskStarted
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.MessageData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun ServiceEventMessage.serviceType() = when (this) {
  is TaskCompletedEvent -> TaskCompleted
  is TaskFailedEvent -> TaskFailed
  is TaskRetriedEvent -> TaskRetried
  is TaskStartedEvent -> TaskStarted
}

@Serializable
sealed interface ServiceEventData : MessageData {
  val taskRetrySequence: Int
  val taskRetryIndex: Int
  val taskMeta: Map<String, ByteArray>
  val taskTags: Set<String>
  val infiniticVersion: String
}

@Serializable
data class TaskStartedData(
  override val taskRetrySequence: Int,
  override val taskRetryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskFailedData(
  val taskError: ErrorData,
  override val taskRetrySequence: Int,
  override val taskRetryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskRetriedData(
  val taskError: ErrorData,
  val taskRetryDelayMillis: Long,
  override val taskRetrySequence: Int,
  override val taskRetryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskCompletedData(
  val taskResult: JsonElement,
  override val taskRetrySequence: Int,
  override val taskRetryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

fun ServiceEventMessage.toServiceData(): ServiceEventData = when (this) {

  is TaskStartedEvent -> TaskStartedData(
      taskRetrySequence = taskRetrySequence.toInt(),
      taskRetryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskRetriedEvent -> TaskRetriedData(
      taskError = lastError.toErrorData(),
      taskRetryDelayMillis = taskRetryDelay.long,
      taskRetrySequence = taskRetrySequence.toInt(),
      taskRetryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskFailedEvent -> TaskFailedData(
      taskError = executionError.toErrorData(),
      taskRetrySequence = taskRetrySequence.toInt(),
      taskRetryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskCompletedEvent -> TaskCompletedData(
      taskResult = returnValue.toJson(),
      taskRetrySequence = taskRetrySequence.toInt(),
      taskRetryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )
}
