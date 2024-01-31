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

import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import io.infinitic.common.utils.JsonAble
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
sealed interface DeferredErrorData : JsonAble {
  override fun toJson() = Json.encodeToJsonElement(this)
}

@Serializable
@SerialName("taskFailed")
data class DeferredTaskFailedData(
  val taskId: String,
  val serviceName: String,
  val taskName: String,
  val error: JsonElement
) : DeferredErrorData

@Serializable
@SerialName("taskCanceled")
data class DeferredTaskCanceledData(
  val taskId: String,
  val serviceName: String,
  val taskName: String
) : DeferredErrorData

@Serializable
@SerialName("taskTimedOut")
data class DeferredTaskTimedOutData(
  val taskId: String,
  val serviceName: String,
  val taskName: String
) : DeferredErrorData

@Serializable
@SerialName("taskUnknown")
data class DeferredTaskUnknownData(
  val taskId: String,
  val serviceName: String,
) : DeferredErrorData

@Serializable
@SerialName("workflowFailed")
data class DeferredWorkflowFailedData(
  val workflowId: String,
  val workflowName: String,
  val workflowMethodId: String?,
  val deferredError: DeferredErrorData
) : DeferredErrorData

@Serializable
@SerialName("workflowCanceled")
data class DeferredWorkflowCanceledData(
  val workflowId: String,
  val workflowName: String,
  val workflowMethodId: String?,
) : DeferredErrorData

@Serializable
@SerialName("workflowTimedOut")
data class DeferredWorkflowTimedOutData(
  val workflowId: String,
  val workflowName: String,
  val workflowMethodId: String?,
) : DeferredErrorData

@Serializable
@SerialName("workflowUnknown")
data class DeferredWorkflowUnknownData(
  val workflowId: String,
  val workflowName: String,
  val workflowMethodId: String?,
) : DeferredErrorData

@Serializable
@SerialName("workflowExecutorFailed")
data class WorkflowTaskErrorData(
  val workflowId: String,
  val workflowName: String,
  val workflowTaskId: String,
  val error: JsonElement
) : DeferredErrorData


fun DeferredError.toDeferredErrorData(): DeferredErrorData = when (this) {
  is WorkflowTaskFailedError -> WorkflowTaskErrorData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowTaskId = workflowTaskId.toString(),
      error = cause.toJson(),
  )

  is TaskFailedError -> DeferredTaskFailedData(
      taskId = taskId.toString(),
      serviceName = serviceName.toString(),
      taskName = methodName.toString(),
      error = cause.toJson(),
  )

  is TaskCanceledError -> DeferredTaskCanceledData(
      taskId = taskId.toString(),
      serviceName = serviceName.toString(),
      taskName = methodName.toString(),
  )

  is TaskTimedOutError -> DeferredTaskTimedOutData(
      taskId = taskId.toString(),
      serviceName = serviceName.toString(),
      taskName = methodName.toString(),
  )

  is TaskUnknownError -> DeferredTaskUnknownData(
      taskId = taskId.toString(),
      serviceName = serviceName.toString(),
  )

  is MethodFailedError -> DeferredWorkflowFailedData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowMethodId = workflowMethodId?.toString(),
      deferredError = deferredError.toDeferredErrorData(),
  )

  is MethodCanceledError -> DeferredWorkflowCanceledData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowMethodId = workflowMethodId?.toString(),
  )

  is MethodTimedOutError -> DeferredWorkflowTimedOutData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowMethodId = workflowMethodId?.toString(),
  )

  is MethodUnknownError -> DeferredWorkflowUnknownData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowMethodId = workflowMethodId?.toString(),
  )
}
