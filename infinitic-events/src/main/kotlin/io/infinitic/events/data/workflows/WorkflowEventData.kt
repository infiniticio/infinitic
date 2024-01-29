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

import io.infinitic.common.workflows.engine.messages.ChildMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.events.InfiniticEventType.WORKFLOW_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_FAILED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TASK_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TIMED_OUT
import io.infinitic.events.data.toData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowEventMessage.workflowType() = when (this) {
  is WorkflowCompletedEvent -> WORKFLOW_COMPLETED
  is WorkflowCanceledEvent -> WORKFLOW_CANCELED
  is MethodDispatchedEvent -> WORKFLOW_METHOD_DISPATCHED
  is MethodCompletedEvent -> WORKFLOW_METHOD_COMPLETED
  is MethodFailedEvent -> WORKFLOW_METHOD_FAILED
  is MethodCanceledEvent -> WORKFLOW_METHOD_CANCELED
  is MethodTimedOutEvent -> WORKFLOW_METHOD_TIMED_OUT
  is TaskDispatchedEvent -> WORKFLOW_METHOD_TASK_DISPATCHED
  is ChildMethodDispatchedEvent -> WORKFLOW_METHOD_CHILD_DISPATCHED
}

@Serializable
sealed interface WorkflowEventData : InfiniticCloudEventsData {
  val workerName: String
}

fun WorkflowEventMessage.toWorkflowData(): InfiniticCloudEventsData = when (this) {

  is WorkflowCompletedEvent -> WorkflowCompletedData(
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is WorkflowCanceledEvent -> WorkflowCanceledData(
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodDispatchedEvent -> WorkflowMethodDispatchedData(
      workflowMethodArgs = methodParameters.toJson(),
      workflowMethodName = methodName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      requester = requester.toData(),
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
      taskDispatched = ChildTaskDispatchedData(
          methodName = taskDispatched.taskName.toString(),
          methodArgs = taskDispatched.methodParameters.toJson(),
          taskId = taskDispatched.taskId.toString(),
          serviceName = taskDispatched.serviceName.toString(),
      ),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is ChildMethodDispatchedEvent -> ChildMethodDispatchedData(
      childWorkflowDispatched = ChildWorkflowDispatchedData(
          methodName = childMethodDispatched.methodName.toString(),
          methodArgs = childMethodDispatched.methodParameters.toJson(),
          workflowId = childMethodDispatched.workflowId.toString(),
          workflowName = childMethodDispatched.workflowName.toString(),
          workflowMethodId = childMethodDispatched.workflowMethodId.toString(),
      ),
      workflowMethodId = workflowMethodId.toString(),
      workerName = workflowName.toString(),
      infiniticVersion = version.toString(),
  )
}

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
  val taskDispatched: ChildTaskDispatchedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData

@Serializable
data class ChildMethodDispatchedData(
  val childWorkflowDispatched: ChildWorkflowDispatchedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEventData
