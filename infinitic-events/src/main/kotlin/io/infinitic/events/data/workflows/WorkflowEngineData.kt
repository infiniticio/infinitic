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
import io.infinitic.events.InfiniticEventType
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_CANCELED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_FAILED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_TIMED_OUT
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_CHILD_UNKNOWN
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TASK_COMPLETED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TASK_FAILED
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TASK_TIMED_OUT
import io.infinitic.events.InfiniticEventType.WORKFLOW_METHOD_TIMER_COMPLETED
import io.infinitic.events.data.ErrorData
import io.infinitic.events.data.toErrorData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WorkflowEngineMessage.workflowType(): InfiniticEventType? = when (this) {
  is WorkflowCmdMessage -> null
  is ChildMethodCompleted -> WORKFLOW_METHOD_CHILD_COMPLETED
  is ChildMethodCanceled -> WORKFLOW_METHOD_CHILD_CANCELED
  is ChildMethodFailed -> WORKFLOW_METHOD_CHILD_FAILED
  is ChildMethodTimedOut -> WORKFLOW_METHOD_CHILD_TIMED_OUT
  is ChildMethodUnknown -> WORKFLOW_METHOD_CHILD_UNKNOWN
  is TaskCompleted -> WORKFLOW_METHOD_TASK_COMPLETED
  is TaskCanceled -> null
  is TaskFailed -> WORKFLOW_METHOD_TASK_FAILED
  is TaskTimedOut -> WORKFLOW_METHOD_TASK_TIMED_OUT
  is TimerCompleted -> WORKFLOW_METHOD_TIMER_COMPLETED
}

fun WorkflowEngineMessage.toWorkflowData(): WorkflowEngineData = when (this) {
  is WorkflowCmdMessage -> thisShouldNotHappen()

  is ChildMethodCompleted -> ChildWorkflowCompletedInWorkflowData(
      childWorkflowCompleted = with(childWorkflowReturnValue) {
        ChildWorkflowCompletedData(
            result = returnValue.toJson(),
            workflowId = workflowId.toString(),
            workflowName = workflowName.toString(),
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
            taskId = taskId.toString(),
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
sealed interface WorkflowEngineData : InfiniticCloudEventsData {
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
  override val workflowId: String,
  override val workflowName: String,
  override val workflowMethodId: String,
) : ChildWorkflowData

@Serializable
data class ChildWorkflowCanceledData(
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
  val serviceName: String
}

@Serializable
data class ChildTaskCompletedData(
  val result: JsonElement,
  override val taskId: String,
  override val serviceName: String,
) : ChildTaskData

@Serializable
data class ChildTaskFailedData(
  val error: ErrorData,
  override val taskId: String,
  override val serviceName: String,
) : ChildTaskData

@Serializable
data class ChildTaskTimedOutData(
  override val taskId: String,
  override val serviceName: String
) : ChildTaskData

@Serializable
data class ChildTimerCompletedData(
  val timerId: String,
)
