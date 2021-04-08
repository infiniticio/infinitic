/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.engine.messages

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowEngineEnvelope(
    val workflowId: WorkflowId,
    val type: WorkflowEngineMessageType,
    val waitWorkflow: WaitWorkflow? = null,
    val cancelWorkflow: CancelWorkflow? = null,
    val sendToChannel: SendToChannel? = null,
    val childWorkflowCanceled: ChildWorkflowCanceled? = null,
    val childWorkflowCompleted: ChildWorkflowCompleted? = null,
    val workflowTaskCompleted: WorkflowTaskCompleted? = null,
    val timerCompleted: TimerCompleted? = null,
    val dispatchWorkflow: DispatchWorkflow? = null,
    val taskCanceled: TaskCanceled? = null,
    val taskCompleted: TaskCompleted? = null
) {
    init {
        val noNull = listOfNotNull(
            waitWorkflow,
            cancelWorkflow,
            sendToChannel,
            childWorkflowCanceled,
            childWorkflowCompleted,
            workflowTaskCompleted,
            timerCompleted,
            dispatchWorkflow,
            taskCanceled,
            taskCompleted
        )

        require(noNull.size == 1) {
            if (noNull.size > 1) {
                "More than 1 message provided: ${noNull.joinToString()}"
            } else {
                "No message provided"
            }
        }

        require(noNull.first() == message()) {
            "Provided type $type inconsistent with message ${noNull.first()}"
        }

        require(noNull.first().workflowId == workflowId) {
            "Provided workflowId $workflowId inconsistent with message ${noNull.first()}"
        }
    }

    companion object {
        fun from(msg: WorkflowEngineMessage) = when (msg) {
            is WaitWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.WAIT_WORKFLOW,
                waitWorkflow = msg
            )
            is CancelWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CANCEL_WORKFLOW,
                cancelWorkflow = msg
            )
            is SendToChannel -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.EMIT_TO_CHANNEL,
                sendToChannel = msg
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
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun message(): WorkflowEngineMessage = when (type) {
        WorkflowEngineMessageType.WAIT_WORKFLOW -> waitWorkflow!!
        WorkflowEngineMessageType.CANCEL_WORKFLOW -> cancelWorkflow!!
        WorkflowEngineMessageType.EMIT_TO_CHANNEL -> sendToChannel!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED -> childWorkflowCanceled!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED -> childWorkflowCompleted!!
        WorkflowEngineMessageType.WORKFLOW_TASK_COMPLETED -> workflowTaskCompleted!!
        WorkflowEngineMessageType.TIMER_COMPLETED -> timerCompleted!!
        WorkflowEngineMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow!!
        WorkflowEngineMessageType.TASK_CANCELED -> taskCanceled!!
        WorkflowEngineMessageType.TASK_COMPLETED -> taskCompleted!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
