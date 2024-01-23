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
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.events.InfiniticWorkflowEventType
import io.infinitic.events.WorkflowMethodChildCompleted
import io.infinitic.events.WorkflowMethodChildFailed
import io.infinitic.events.WorkflowMethodChildTimedOut
import io.infinitic.events.WorkflowMethodTaskCompleted
import io.infinitic.events.WorkflowMethodTaskFailed
import io.infinitic.events.WorkflowMethodTaskTimedOut
import io.infinitic.events.WorkflowMethodTimerCompleted
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowEngineMessage.workflowType(): InfiniticWorkflowEventType? = when (this) {
  is WorkflowCmdMessage -> null
  is ChildMethodCompleted -> WorkflowMethodChildCompleted
  is ChildMethodCanceled -> TODO()
  is ChildMethodFailed -> WorkflowMethodChildFailed
  is ChildMethodTimedOut -> WorkflowMethodChildTimedOut
  is ChildMethodUnknown -> TODO()
  is TaskCompleted -> WorkflowMethodTaskCompleted
  is TaskCanceled -> TODO()
  is TaskFailed -> WorkflowMethodTaskFailed
  is TaskTimedOut -> WorkflowMethodTaskTimedOut
  is TimerCompleted -> WorkflowMethodTimerCompleted
}

fun WorkflowEngineMessage.toWorkflowData(): WorkflowEngineData? = when (this) {
  is WorkflowCmdMessage -> thisShouldNotHappen()
  is ChildMethodCompleted -> TODO()
  is ChildMethodCanceled -> TODO()
  is ChildMethodFailed -> TODO()
  is ChildMethodTimedOut -> TODO()
  is ChildMethodUnknown -> TODO()

  is TaskCompleted -> WorkflowMethodTaskCompletedData(
      taskResult = taskReturnValue.returnValue.toJson(),
      taskId = taskReturnValue.taskId.toString(),
      serviceMethodName = taskReturnValue.taskMethodName.toString(),
      serviceName = taskReturnValue.serviceName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskCanceled -> TODO()

  is TaskFailed -> WorkflowMethodTaskFailedData(
      taskError = taskFailedError.cause.toErrorData(),
      taskId = taskFailedError.taskId.toString(),
      serviceMethodName = taskFailedError.taskMethodName.toString(),
      serviceName = taskFailedError.serviceName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskTimedOut -> WorkflowMethodTaskTimedOutData(
      taskId = taskTimedOutError.taskId.toString(),
      serviceMethodName = taskTimedOutError.methodName.toString(),
      serviceName = taskTimedOutError.serviceName.toString(),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TimerCompleted -> TODO()
}

@Serializable
sealed interface WorkflowEngineData : WorkflowEventData {
  val workerName: String
}

@Serializable
data class WorkflowMethodTaskCompletedData(
  val taskResult: JsonElement,
  val taskId: String,
  val serviceMethodName: String,
  val serviceName: String,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodTaskFailedData(
  val taskError: ErrorData,
  val taskId: String,
  val serviceMethodName: String,
  val serviceName: String,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class WorkflowMethodTaskTimedOutData(
  val taskId: String,
  val serviceMethodName: String,
  val serviceName: String,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData
