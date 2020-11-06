// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.messages

import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowEngineEnvelope(
    val workflowId: WorkflowId,
    val type: WorkflowEngineMessageType,
    val cancelWorkflow: CancelWorkflow? = null,
    val childWorkflowCanceled: ChildWorkflowCanceled? = null,
    val childWorkflowCompleted: ChildWorkflowCompleted? = null,
    val workflowTaskCompleted: WorkflowTaskCompleted? = null,
    val workflowTaskDispatched: WorkflowTaskDispatched? = null,
    val timerCompleted: TimerCompleted? = null,
    val dispatchWorkflow: DispatchWorkflow? = null,
    val objectReceived: ObjectReceived? = null,
    val taskCanceled: TaskCanceled? = null,
    val taskCompleted: TaskCompleted? = null,
    val taskDispatched: TaskDispatched? = null,
    val workflowCanceled: WorkflowCanceled? = null,
    val workflowCompleted: WorkflowCompleted? = null
) {
    init {
        val noNull = listOfNotNull(
            cancelWorkflow,
            childWorkflowCanceled,
            childWorkflowCompleted,
            workflowTaskCompleted,
            workflowTaskDispatched,
            timerCompleted,
            dispatchWorkflow,
            objectReceived,
            taskCanceled,
            taskCompleted,
            taskDispatched,
            workflowCanceled,
            workflowCompleted
        )

        require(noNull.size == 1) {
            if (noNull.size > 1) {
                "More than 1 message provided: ${noNull.joinToString()}"
            } else {
                "No message provided"
            }
        }

        require(noNull.first() == value()) {
            "Provided type $type inconsistent with message ${noNull.first()}"
        }

        require(noNull.first().workflowId == workflowId) {
            "Provided workflowId $workflowId inconsistent with message ${noNull.first()}"
        }
    }

    companion object {
        fun from(msg: WorkflowEngineMessage) = when (msg) {
            is CancelWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CANCEL_WORKFLOW,
                cancelWorkflow = msg
            )
            is ChildWorkflowCanceled -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED,
                childWorkflowCanceled = msg
            )
            is ChildWorkflowCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED,
                childWorkflowCompleted = msg
            )
            is WorkflowTaskCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.WORKFLOW_TASK_COMPLETED,
                workflowTaskCompleted = msg
            )
            is WorkflowTaskDispatched -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.WORKFLOW_TASK_DISPATCHED,
                workflowTaskDispatched = msg
            )
            is TimerCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TIMER_COMPLETED,
                timerCompleted = msg
            )
            is DispatchWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.DISPATCH_WORKFLOW,
                dispatchWorkflow = msg
            )
            is ObjectReceived -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.OBJECT_RECEIVED,
                objectReceived = msg
            )
            is TaskCanceled -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_CANCELED,
                taskCanceled = msg
            )
            is TaskCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_COMPLETED,
                taskCompleted = msg
            )
            is TaskDispatched -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_DISPATCHED,
                taskDispatched = msg
            )
            is WorkflowCanceled -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.WORKFLOW_CANCELED,
                workflowCanceled = msg
            )
            is WorkflowCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.WORKFLOW_COMPLETED,
                workflowCompleted = msg
            )
        }
    }

    fun value(): WorkflowEngineMessage = when (type) {
        WorkflowEngineMessageType.CANCEL_WORKFLOW -> cancelWorkflow!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED -> childWorkflowCanceled!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED -> childWorkflowCompleted!!
        WorkflowEngineMessageType.WORKFLOW_TASK_COMPLETED -> workflowTaskCompleted!!
        WorkflowEngineMessageType.WORKFLOW_TASK_DISPATCHED -> workflowTaskDispatched!!
        WorkflowEngineMessageType.TIMER_COMPLETED -> timerCompleted!!
        WorkflowEngineMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow!!
        WorkflowEngineMessageType.OBJECT_RECEIVED -> objectReceived!!
        WorkflowEngineMessageType.TASK_CANCELED -> taskCanceled!!
        WorkflowEngineMessageType.TASK_COMPLETED -> taskCompleted!!
        WorkflowEngineMessageType.TASK_DISPATCHED -> taskDispatched!!
        WorkflowEngineMessageType.WORKFLOW_CANCELED -> workflowCanceled!!
        WorkflowEngineMessageType.WORKFLOW_COMPLETED -> workflowCompleted!!
    }
}
