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
import io.infinitic.events.ChildMethodCanceledType
import io.infinitic.events.ChildMethodCompletedType
import io.infinitic.events.ChildMethodFailedType
import io.infinitic.events.ChildMethodTimedOutType
import io.infinitic.events.ChildMethodUnknownType
import io.infinitic.events.ChildTaskCompletedType
import io.infinitic.events.ChildTaskFailedType
import io.infinitic.events.ChildTaskTimedOutType
import io.infinitic.events.ChildTimerCompletedType
import io.infinitic.events.InfiniticWorkflowEventType
import io.infinitic.events.data.DeferredErrorData
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.RequesterData
import io.infinitic.events.data.toDeferredErrorData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

private const val UNDEFINED = "Undefined"
fun WorkflowEngineMessage.workflowType(): InfiniticWorkflowEventType? = when (this) {
  is WorkflowCmdMessage -> null
  is ChildMethodCompleted -> ChildMethodCompletedType
  is ChildMethodCanceled -> ChildMethodCanceledType
  is ChildMethodFailed -> ChildMethodFailedType
  is ChildMethodTimedOut -> ChildMethodTimedOutType
  is ChildMethodUnknown -> ChildMethodUnknownType
  is TaskCompleted -> ChildTaskCompletedType
  is TaskCanceled -> null
  is TaskFailed -> ChildTaskFailedType
  is TaskTimedOut -> ChildTaskTimedOutType
  is TimerCompleted -> ChildTimerCompletedType
}

fun WorkflowEngineMessage.toWorkflowData(): WorkflowEngineData = when (this) {
  is WorkflowCmdMessage -> thisShouldNotHappen()

  is ChildMethodCompleted -> ChildWorkflowCompletedInWorkflowData(
      childWorkflowCompleted = with(childWorkflowReturnValue) {
        ChildWorkflowCompletedData(
            result = returnValue.toJson(),
            workflowId = workflowId.toString(),
            workflowName = workflowName?.toString() ?: UNDEFINED,
            workflowMethodId = workflowMethodId.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is ChildMethodCanceled -> ChildWorkflowCanceledInWorkflowData(
      childWorkflowCanceled = with(childMethodCanceledError) {
        ChildWorkflowCanceledData(
            requester = TODO(),
            workflowId = workflowId.toString(),
            workflowName = workflowName.toString(),
            workflowMethodId = workflowMethodId.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is ChildMethodFailed -> ChildWorkflowFailedInWorkflowData(
      childWorkflowFailed = with(childMethodFailedError) {
        ChildWorkflowFailedData(
            error = deferredError.toDeferredErrorData(),
            workflowId = workflowId.toString(),
            workflowName = workflowName.toString(),
            workflowMethodId = workflowMethodId.toString(),
        )
      },

      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is ChildMethodTimedOut -> ChildWorkflowTimedOutInWorkflowData(
      childWorkflowTimedOut = with(childMethodTimedOutError) {
        ChildWorkflowTimedOutData(
            timeoutMillis = timeout.long,
            workflowId = workflowId.toString(),
            workflowName = workflowName.toString(),
            workflowMethodId = workflowMethodId.toString(),
        )
      },

      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is ChildMethodUnknown -> ChildWorkflowUnknownInWorkflowData(
      childWorkflowUnknown = with(childMethodUnknownError) {
        ChildWorkflowUnknownData(
            workflowId = workflowId.toString(),
            workflowName = workflowName.toString(),
            workflowMethodId = workflowMethodId.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskCompleted -> TaskCompleteInWorkflowData(
      taskCompleted = with(taskReturnValue) {
        ChildTaskCompletedData(
            result = returnValue.toJson(),
            taskId = taskId.toString(),
            taskName = taskMethodName?.toString() ?: UNDEFINED,
            serviceName = serviceName.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskCanceled -> TODO()

  is TaskFailed -> TaskFailedInWorkflowData(
      taskFailed = with(taskFailedError) {
        ChildTaskFailedData(
            error = cause.toErrorData(),
            taskId = taskId.toString(),
            taskName = taskName.toString(),
            serviceName = serviceName.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TaskTimedOut -> TaskTimedOutInWorkflowData(
      taskTimedOut = with(taskTimedOutError) {
        ChildTaskTimedOutData(
            timeoutMillis = timeout.long,
            taskId = taskId.toString(),
            taskName = taskName.toString(),
            serviceName = serviceName.toString(),
        )
      },
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )

  is TimerCompleted -> TimerCompletedInWorkflowData(
      timerCompleted = ChildTimerCompletedData(
          timerId = timerId.toString(),
      ),
      workflowMethodId = workflowMethodId.toString(),
      workerName = emitterName.toString(),
      infiniticVersion = version.toString(),
  )
}

@Serializable
sealed interface WorkflowEngineData : WorkflowEventData {
  val workerName: String
}

@Serializable
data class TaskCompleteInWorkflowData(
  val taskCompleted: ChildTaskCompletedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class TaskFailedInWorkflowData(
  val taskFailed: ChildTaskFailedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class TaskTimedOutInWorkflowData(
  val taskTimedOut: ChildTaskTimedOutData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData


@Serializable
data class TimerCompletedInWorkflowData(
  val timerCompleted: ChildTimerCompletedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class ChildWorkflowCompletedInWorkflowData(
  val childWorkflowCompleted: ChildWorkflowCompletedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class ChildWorkflowFailedInWorkflowData(
  val childWorkflowFailed: ChildWorkflowFailedData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class ChildWorkflowTimedOutInWorkflowData(
  val childWorkflowTimedOut: ChildWorkflowTimedOutData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class ChildWorkflowCanceledInWorkflowData(
  val childWorkflowCanceled: ChildWorkflowCanceledData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
data class ChildWorkflowUnknownInWorkflowData(
  val childWorkflowUnknown: ChildWorkflowUnknownData,
  val workflowMethodId: String,
  override val workerName: String,
  override val infiniticVersion: String
) : WorkflowEngineData

@Serializable
sealed interface ChildWorkflowData {
  val workflowId: String
  val workflowName: String
  val workflowMethodId: String
}

@Serializable
data class ChildWorkflowCompletedData(
  val result: JsonElement,
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
data class ChildWorkflowFailedData(
  val error: DeferredErrorData,
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
data class ChildWorkflowTimedOutData(
  val timeoutMillis: Long,
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
data class ChildWorkflowCanceledData(
  val requester: RequesterData,
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
data class ChildWorkflowUnknownData(
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
sealed interface ChildTaskData {
  val taskId: String
  val taskName: String
  val serviceName: String
}

@Serializable
data class ChildTaskCompletedData(
  val result: JsonElement,
  override val taskId: String,
  override val taskName: String,
  override val serviceName: String,
) : ChildTaskData

@Serializable
data class ChildTaskFailedData(
  val error: ErrorData,
  override val taskId: String,
  override val taskName: String,
  override val serviceName: String,
) : ChildTaskData

@Serializable
data class ChildTaskTimedOutData(
  val timeoutMillis: Long,
  override val taskId: String,
  override val taskName: String,
  override val serviceName: String
) : ChildTaskData

@Serializable
data class ChildTimerCompletedData(
  val timerId: String,
)
