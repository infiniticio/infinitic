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

package io.infinitic.events.data

import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.events.InfiniticEventType

fun ServiceEventMessage.type() = when (this) {
  is TaskCompletedEvent -> InfiniticEventType.TASK_COMPLETED
  is TaskFailedEvent -> InfiniticEventType.TASK_FAILED
  is TaskRetriedEvent -> InfiniticEventType.TASK_RETRIED
  is TaskStartedEvent -> InfiniticEventType.TASK_STARTED
}

sealed interface ServiceEventData : MessageData

data class TaskCompletedData(
  val retrySequence: Int,
  val retryIndex: Int,
  val returnValue: String,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
  val workerName: String
) : ServiceEventData

data class TaskFailedData(
  val retrySequence: Int,
  val retryIndex: Int,
  val error: ErrorData,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
  val workerName: String
) : ServiceEventData

data class TaskRetriedData(
  val retrySequence: Int,
  val retryIndex: Int,
  val error: ErrorData,
  val delayMillis: Long,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
  val workerName: String
) : ServiceEventData

data class TaskStartedData(
  val retrySequence: Int,
  val retryIndex: Int,
  val meta: Map<String, ByteArray>,
  val tags: Set<String>,
  val workerName: String
) : ServiceEventData


fun ServiceEventMessage.toData(): ServiceEventData = when (this) {

  is TaskCompletedEvent -> TaskCompletedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      returnValue = returnValue.serializedData.toJson(),
      meta = taskMeta.map,
      tags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
  )

  is TaskFailedEvent -> TaskFailedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      error = executionError.toErrorEvent(),
      meta = taskMeta.map,
      tags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
  )

  is TaskRetriedEvent -> TaskRetriedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      error = lastError.toErrorEvent(),
      delayMillis = taskRetryDelay.long,
      meta = taskMeta.map,
      tags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
  )

  is TaskStartedEvent -> TaskStartedData(
      retrySequence = taskRetrySequence.toInt(),
      retryIndex = taskRetryIndex.toInt(),
      meta = taskMeta.map,
      tags = taskTags.map { it.toString() }.toSet(),
      workerName = emitterName.toString(),
  )
}
