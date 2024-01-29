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
import io.infinitic.common.workflows.data.workflows.set
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.events.InfiniticEventType
import io.infinitic.events.InfiniticEventType.WORKFLOW_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_EXECUTOR_RETRY_REQUESTED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_DISPATCHED
import io.infinitic.events.InfiniticEventType.WORKFLOW_SIGNALED
import io.infinitic.events.InfiniticEventType.WORKFLOW_TASKS_RETRY_REQUESTED
import io.infinitic.events.data.RequesterData
import io.infinitic.events.data.toData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowCmdMessage.workflowType(): InfiniticEventType? = when (this) {
  is DispatchNewWorkflow -> WORKFLOW_DISPATCHED
  is DispatchMethod -> WORKFLOW_METHOD_DISPATCHED
  is CancelWorkflow -> WORKFLOW_CANCELED
  is CompleteTimers -> null
  is CompleteWorkflow -> null
  is RetryTasks -> WORKFLOW_TASKS_RETRY_REQUESTED
  is RetryWorkflowTask -> WORKFLOW_EXECUTOR_RETRY_REQUESTED
  is SendSignal -> WORKFLOW_SIGNALED
  is WaitWorkflow -> null
}

fun WorkflowCmdMessage.toWorkflowData(): WorkflowCmdData = when (this) {
  is DispatchNewWorkflow -> WorkflowDispatchedData(
      workflowMeta = workflowMeta.map,
      workflowTags = workflowTags.set,
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is DispatchMethod -> WorkflowMethodDispatchedData(
      workflowMethodArgs = methodParameters.toJson(),
      workflowMethodName = methodName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is CancelWorkflow -> WorkflowCancelRequestedData(
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is CompleteTimers -> TODO()

  is CompleteWorkflow -> TODO()

  is RetryTasks -> TaskRetryRequestedData(
      taskId = taskId?.toString(),
      taskStatus = taskStatus?.toString(),
      serviceName = serviceName?.toString(),
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is RetryWorkflowTask -> WorkflowTaskRetryRequestedData(
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is SendSignal -> WorkflowSignalSentData(
      channelName = channelName.toString(),
      signalId = signalId.toString(),
      signalArg = signalData.serializedData.toJson(),
      requester = (requester ?: thisShouldNotHappen()).toData(),
      infiniticVersion = version.toString(),
  )

  is WaitWorkflow -> TODO()
}

@Serializable
sealed interface WorkflowCmdData : InfiniticCloudEventsData {
  val requester: RequesterData
}

@Serializable
data class WorkflowDispatchedData(
  val workflowMeta: Map<String, ByteArray>,
  val workflowTags: Set<String>,
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

@Serializable
data class WorkflowMethodDispatchedData(
  val workflowMethodArgs: List<JsonElement>,
  val workflowMethodName: String,
  val workflowMethodId: String,
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

@Serializable
data class WorkflowCancelRequestedData(
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

@Serializable
data class WorkflowTaskRetryRequestedData(
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

@Serializable
data class TaskRetryRequestedData(
  val taskId: String?,
  val taskStatus: String?,
  val serviceName: String?,
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

@Serializable
data class WorkflowSignalSentData(
  val channelName: String,
  val signalId: String,
  val signalArg: JsonElement,
  override val requester: RequesterData,
  override val infiniticVersion: String
) : WorkflowCmdData

