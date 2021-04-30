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

package io.infinitic.common.clients.messages

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.messages.Envelope
import kotlinx.serialization.Serializable

@Serializable
data class ClientEnvelope(
    private val clientName: ClientName,
    private val type: ClientMessageType,
    private val taskCompleted: TaskCompleted? = null,
    private val taskCanceled: TaskCanceled? = null,
    private val taskFailed: TaskFailed? = null,
    private val unknownTask: UnknownTask? = null,
    private val taskIdsPerTag: TaskIdsPerTag? = null,
    private val workflowCompleted: WorkflowCompleted? = null,
    private val workflowCanceled: WorkflowCanceled? = null,
    private val workflowFailed: WorkflowFailed? = null,
    private val unknownWorkflow: UnknownWorkflow? = null,
    private val workflowAlreadyCompleted: WorkflowAlreadyCompleted? = null,
    private val workflowIdsPerTag: WorkflowIdsPerTag? = null
) : Envelope<ClientMessage> {
    init {
        val noNull = listOfNotNull(
            taskCompleted,
            taskCanceled,
            taskFailed,
            unknownTask,
            taskIdsPerTag,
            workflowCompleted,
            workflowCanceled,
            workflowFailed,
            unknownWorkflow,
            workflowAlreadyCompleted,
            workflowIdsPerTag
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require(noNull.first().clientName == clientName)
    }

    companion object {
        fun from(msg: ClientMessage) = when (msg) {
            is TaskCompleted -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.TASK_COMPLETED,
                taskCompleted = msg
            )
            is TaskCanceled -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.TASK_CANCELED,
                taskCanceled = msg
            )
            is TaskFailed -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.TASK_FAILED,
                taskFailed = msg
            )
            is UnknownTask -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.UNKNOWN_TASK,
                unknownTask = msg
            )
            is TaskIdsPerTag -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.TASK_IDS_PER_TAG,
                taskIdsPerTag = msg
            )
            is WorkflowCompleted -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_COMPLETED,
                workflowCompleted = msg
            )
            is WorkflowCanceled -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_CANCELED,
                workflowCanceled = msg
            )
            is WorkflowFailed -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_FAILED,
                workflowFailed = msg
            )
            is UnknownWorkflow -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.UNKNOWN_WORKFLOW,
                unknownWorkflow = msg
            )
            is WorkflowAlreadyCompleted -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_ALREADY_COMPLETED,
                workflowAlreadyCompleted = msg
            )
            is WorkflowIdsPerTag -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_IDS_PER_TAG,
                workflowIdsPerTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message(): ClientMessage = when (type) {
        ClientMessageType.UNKNOWN_TASK -> unknownTask!!
        ClientMessageType.TASK_COMPLETED -> taskCompleted!!
        ClientMessageType.TASK_CANCELED -> taskCanceled!!
        ClientMessageType.TASK_FAILED -> taskFailed!!
        ClientMessageType.TASK_IDS_PER_TAG -> taskIdsPerTag!!
        ClientMessageType.WORKFLOW_COMPLETED -> workflowCompleted!!
        ClientMessageType.WORKFLOW_CANCELED -> workflowCanceled!!
        ClientMessageType.WORKFLOW_FAILED -> workflowFailed!!
        ClientMessageType.UNKNOWN_WORKFLOW -> unknownWorkflow!!
        ClientMessageType.WORKFLOW_ALREADY_COMPLETED -> workflowAlreadyCompleted!!
        ClientMessageType.WORKFLOW_IDS_PER_TAG -> workflowIdsPerTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
