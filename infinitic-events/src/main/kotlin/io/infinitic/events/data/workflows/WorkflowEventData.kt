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

package io.infinitic.events.data.workflows

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.events.WorkflowMethodTaskCompleted
import io.infinitic.events.WorkflowMethodTaskFailed
import io.infinitic.events.data.MessageData
import kotlinx.serialization.Serializable

fun ServiceEventMessage.workflowType() = when (this) {
  is TaskCompletedEvent -> WorkflowMethodTaskCompleted
  is TaskFailedEvent -> WorkflowMethodTaskFailed
  is TaskRetriedEvent -> thisShouldNotHappen()
  is TaskStartedEvent -> thisShouldNotHappen()
}

@Serializable
sealed interface WorkflowEventData : MessageData {
  val infiniticVersion: String
}

sealed interface WorkflowMethodEventData : WorkflowEventData {
  val workflowMethodId: String
  val workflowMethodName: String
}

sealed interface WorkflowTaskEventData : WorkflowMethodEventData {
  val workflowTaskId: String
  val workflowVersion: String?
}

sealed interface WorkflowMethodTaskEventData : WorkflowMethodEventData {
  val serviceName: String
  val taskName: String
  val taskId: String
}

sealed interface WorkflowMethodTimerEventData : WorkflowMethodEventData {
  val timerID: String
}

sealed interface WorkflowMethodChildEventData : WorkflowMethodEventData {
  val childWorkflowId: String
  val childWorkflowName: String
  val childWorkflowMethodId: String
  val childWorkflowMethodName: String
}


//@Serializable
//data class WorkflowMethodTaskCompletedData(
//  val error: ErrorData,
//  override val retrySequence: Int,
//  override val retryIndex: Int,
//  override val meta: Map<String, ByteArray>,
//  override val tags: Set<String>,
//  val workerName: String,
//  override val infiniticVersion: String
//) : ServiceEventData
//
//@Serializable
//data class WorkflowMethodTaskFailedData(
//  val error: ErrorData,
//  val delayMillis: Long,
//  override val retrySequence: Int,
//  override val retryIndex: Int,
//  override val meta: Map<String, ByteArray>,
//  override val tags: Set<String>,
//  val workerName: String,
//  override val infiniticVersion: String
//) : ServiceEventData
//
//@Serializable
//data class WorkflowMethodTaskTimedOutData(
//  val returnValue: JsonElement,
//  override val retrySequence: Int,
//  override val retryIndex: Int,
//  override val meta: Map<String, ByteArray>,
//  override val tags: Set<String>,
//  val workerName: String,
//  override val infiniticVersion: String
//) : ServiceEventData
//
fun ServiceEventMessage.toWorkflowData(): WorkflowEventData = when (this) {

  is TaskStartedEvent -> thisShouldNotHappen()

  is TaskRetriedEvent -> thisShouldNotHappen()

  is TaskFailedEvent -> thisShouldNotHappen()


  is TaskCompletedEvent -> thisShouldNotHappen()
}
