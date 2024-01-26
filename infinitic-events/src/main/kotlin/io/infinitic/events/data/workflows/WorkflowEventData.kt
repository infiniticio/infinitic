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
import io.infinitic.common.workflows.engine.messages.MethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodStartedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStartedEvent
import io.infinitic.events.ChildTaskDispatchedType
import io.infinitic.events.WorkflowCanceledType
import io.infinitic.events.WorkflowCompletedType
import io.infinitic.events.WorkflowMethodCanceledType
import io.infinitic.events.WorkflowMethodCompletedType
import io.infinitic.events.WorkflowMethodDispatchedType
import io.infinitic.events.WorkflowMethodFailedType
import io.infinitic.events.WorkflowMethodStartedType
import io.infinitic.events.WorkflowMethodTimedOutType
import io.infinitic.events.WorkflowStartedType
import io.infinitic.events.data.MessageData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowEventMessage.workflowType() = when (this) {
  is WorkflowStartedEvent -> WorkflowStartedType
  is WorkflowCompletedEvent -> WorkflowCompletedType
  is WorkflowCanceledEvent -> WorkflowCanceledType
  is MethodStartedEvent -> WorkflowMethodStartedType
  is MethodCompletedEvent -> WorkflowMethodCompletedType
  is MethodFailedEvent -> WorkflowMethodFailedType
  is MethodCanceledEvent -> WorkflowMethodCanceledType
  is MethodTimedOutEvent -> WorkflowMethodTimedOutType
  is MethodDispatchedEvent -> WorkflowMethodDispatchedType
  is TaskDispatchedEvent -> ChildTaskDispatchedType
}

@Serializable
sealed interface WorkflowEventData : MessageData {
  val infiniticVersion: String
}

sealed interface WorkflowMethodEventData : WorkflowEventData {
  val workflowMethodId: String
  val workflowMethodName: String
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

  is MethodStartedEvent -> WorkflowMethodStartedData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodCompletedEvent -> WorkflowMethodCompletedData(
      result = returnValue.toJson(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodFailedEvent -> WorkflowMethodFailedData(
      error = deferredError.toDeferredErrorData(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodCanceledEvent -> WorkflowMethodCanceledData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodTimedOutEvent -> WorkflowMethodTimedOutData(
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is MethodDispatchedEvent -> TODO()
  is TaskDispatchedEvent -> TODO()
}

@Serializable
data class WorkflowStartedData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowCompletedData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowCanceledData(
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodStartedData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodCompletedData(
  val result: JsonElement,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodFailedData(
  val error: DeferredErrorData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodCanceledData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodTimedOutData(
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData
