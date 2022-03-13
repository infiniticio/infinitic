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

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable

@Serializable @AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowEngineEnvelope(
    val workflowId: WorkflowId,
    val type: WorkflowEngineMessageType,
    val dispatchWorkflow: DispatchWorkflow? = null,
    val dispatchMethod: DispatchMethod? = null,
    val waitWorkflow: WaitWorkflow? = null,
    val cancelWorkflow: CancelWorkflow? = null,
    val retryWorkflowTask: RetryWorkflowTask? = null,
    val retryFailedTasks: RetryFailedTasks? = null,
    val completeWorkflow: CompleteWorkflow? = null,
    val sendSignal: SendSignal? = null,
    val timerCompleted: TimerCompleted? = null,
    val childMethodUnknown: ChildMethodUnknown? = null,
    val childMethodCanceled: ChildMethodCanceled? = null,
    val childMethodFailed: ChildMethodFailed? = null,
    val childMethodCompleted: ChildMethodCompleted? = null,
    val taskCanceled: TaskCanceled? = null,
    val taskFailed: TaskFailed? = null,
    val taskCompleted: TaskCompleted? = null
) : Envelope<WorkflowEngineMessage> {
    init {
        val noNull = listOfNotNull(
            dispatchWorkflow,
            dispatchMethod,
            waitWorkflow,
            cancelWorkflow,
            retryWorkflowTask,
            retryFailedTasks,
            completeWorkflow,
            sendSignal,
            timerCompleted,
            childMethodUnknown,
            childMethodFailed,
            childMethodCanceled,
            childMethodCompleted,
            taskCanceled,
            taskFailed,
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
            is DispatchWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.DISPATCH_WORKFLOW,
                dispatchWorkflow = msg
            )
            is DispatchMethod -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.DISPATCH_METHOD,
                dispatchMethod = msg
            )
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
            is RetryWorkflowTask -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.RETRY_WORKFLOW_TASK,
                retryWorkflowTask = msg
            )
            is RetryFailedTasks -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.RETRY_FAILED_TASKS,
                retryFailedTasks = msg
            )
            is CompleteWorkflow -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.COMPLETE_WORKFLOW,
                completeWorkflow = msg
            )
            is SendSignal -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.SEND_SIGNAL,
                sendSignal = msg
            )
            is TimerCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TIMER_COMPLETED,
                timerCompleted = msg
            )
            is ChildMethodUnknown -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_UNKNOWN,
                childMethodUnknown = msg
            )
            is ChildMethodCanceled -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED,
                childMethodCanceled = msg
            )
            is ChildMethodFailed -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_FAILED,
                childMethodFailed = msg
            )
            is ChildMethodCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED,
                childMethodCompleted = msg
            )
            is TaskCanceled -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_CANCELED,
                taskCanceled = msg
            )
            is TaskFailed -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_FAILED,
                taskFailed = msg
            )
            is TaskCompleted -> WorkflowEngineEnvelope(
                msg.workflowId,
                WorkflowEngineMessageType.TASK_COMPLETED,
                taskCompleted = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message(): WorkflowEngineMessage = when (type) {
        WorkflowEngineMessageType.DISPATCH_WORKFLOW -> dispatchWorkflow!!
        WorkflowEngineMessageType.DISPATCH_METHOD -> dispatchMethod!!
        WorkflowEngineMessageType.WAIT_WORKFLOW -> waitWorkflow!!
        WorkflowEngineMessageType.CANCEL_WORKFLOW -> cancelWorkflow!!
        WorkflowEngineMessageType.RETRY_WORKFLOW_TASK -> retryWorkflowTask!!
        WorkflowEngineMessageType.RETRY_FAILED_TASKS -> retryFailedTasks!!
        WorkflowEngineMessageType.COMPLETE_WORKFLOW -> completeWorkflow!!
        WorkflowEngineMessageType.SEND_SIGNAL -> sendSignal!!
        WorkflowEngineMessageType.TIMER_COMPLETED -> timerCompleted!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_UNKNOWN -> childMethodUnknown!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_CANCELED -> childMethodCanceled!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_FAILED -> childMethodFailed!!
        WorkflowEngineMessageType.CHILD_WORKFLOW_COMPLETED -> childMethodCompleted!!
        WorkflowEngineMessageType.TASK_CANCELED -> taskCanceled!!
        WorkflowEngineMessageType.TASK_FAILED -> taskFailed!!
        WorkflowEngineMessageType.TASK_COMPLETED -> taskCompleted!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
