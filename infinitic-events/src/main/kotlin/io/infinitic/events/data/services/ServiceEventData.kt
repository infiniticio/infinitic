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
import io.infinitic.events.InfiniticEventType.TASK_COMPLETED
import io.infinitic.events.InfiniticEventType.TASK_FAILED
import io.infinitic.events.InfiniticEventType.TASK_RETRIED
import io.infinitic.events.InfiniticEventType.TASK_STARTED
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.MessageData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun ServiceEventMessage.serviceType() = when (this) {
  is TaskCompletedEvent -> TASK_COMPLETED
  is TaskFailedEvent -> TASK_FAILED
  is TaskRetriedEvent -> TASK_RETRIED
  is TaskStartedEvent -> TASK_STARTED
}

@Serializable
sealed interface ServiceEventData : MessageData {
  val retrySequence: Int
  val retryIndex: Int
  val taskMeta: Map<String, ByteArray>
  val taskTags: Set<String>
  val infiniticVersion: String
}

@Serializable
data class TaskStartedData(
  override val retrySequence: Int,
  override val retryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskFailedData(
  val error: ErrorData,
  override val retrySequence: Int,
  override val retryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskRetriedData(
  val error: ErrorData,
  val delayMillis: Long,
  override val retrySequence: Int,
  override val retryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

@Serializable
data class TaskCompletedData(
  val returnValue: JsonElement,
  override val retrySequence: Int,
  override val retryIndex: Int,
  override val taskMeta: Map<String, ByteArray>,
  override val taskTags: Set<String>,
  val workerName: String,
  override val infiniticVersion: String
) : ServiceEventData

fun ServiceEventMessage.toServiceData(): ServiceEventData = when (this) {

  is TaskStartedEvent -> TaskStartedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskRetriedEvent -> TaskRetriedData(
      error = lastError.toErrorData(),
      delayMillis = taskRetryDelay.long,
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskFailedEvent -> TaskFailedData(
      error = executionError.toErrorData(),
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskCompletedEvent -> TaskCompletedData(
      returnValue = returnValue.toJson(),
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      taskMeta = taskMeta.map,
      taskTags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )
}
