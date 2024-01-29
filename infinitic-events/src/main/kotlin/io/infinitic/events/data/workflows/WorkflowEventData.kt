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

import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodStartedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStartedEvent
import io.infinitic.events.InfiniticEventType.WORKFLOW_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_FAILED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_STARTED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TASK_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TIMED_OUT
import io.infinitic.events.InfiniticEventType.WORKFLOW_STARTED
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowEventMessage.workflowType() = when (this) {
  is WorkflowStartedEvent -> WORKFLOW_STARTED
  is WorkflowCompletedEvent -> WORKFLOW_COMPLETED
  is WorkflowCanceledEvent -> WORKFLOW_CANCELED
  is MethodStartedEvent -> WORKFLOW_METHOD_STARTED
  is MethodCompletedEvent -> WORKFLOW_METHOD_COMPLETED
  is MethodFailedEvent -> WORKFLOW_METHOD_FAILED
  is MethodCanceledEvent -> WORKFLOW_METHOD_CANCELED
  is MethodTimedOutEvent -> WORKFLOW_METHOD_TIMED_OUT
  is TaskDispatchedEvent -> WORKFLOW_METHOD_TASK_DISPATCHED
}

@Serializable
sealed interface WorkflowEventData : InfiniticCloudEventsData {
  val workerName: String
}

fun WorkflowEventMessage.toWorkflowData(): WorkflowEventData = when (this) {

  is WorkflowStartedEvent -> WorkflowStartedData(
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is WorkflowCompletedEvent -> WorkflowCompletedData(
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is WorkflowCanceledEvent -> WorkflowCanceledData(
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodStartedEvent -> MethodStartedData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodCompletedEvent -> MethodCompletedData(
      result = returnValue.toJson(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodFailedEvent -> MethodFailedData(
      error = deferredError.toDeferredErrorData(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodCanceledEvent -> MethodCanceledData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodTimedOutEvent -> MethodTimedOutData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskDispatchedEvent -> TaskDispatchedData(
      taskId = taskId.toString(),
      taskName = methodName.toString(),
      taskArgs = methodParameters.toJson(),
      serviceName = serviceName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )
}

@Serializable
data class WorkflowStartedData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class WorkflowCompletedData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class WorkflowCanceledData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class MethodStartedData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class MethodCompletedData(
  val result: JsonElement,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class MethodFailedData(
  val error: DeferredErrorData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class MethodCanceledData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class MethodTimedOutData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class TaskDispatchedData(
  val taskId: String,
  val taskName: String,
  val taskArgs: List<JsonElement>,
  val serviceName: String,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData
