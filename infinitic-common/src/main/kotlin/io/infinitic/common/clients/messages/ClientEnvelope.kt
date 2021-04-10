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
    val clientName: ClientName,
    val type: ClientMessageType,
    val taskCompleted: TaskCompleted? = null,
    val unknownTaskWaited: UnknownTaskWaited? = null,
    val workflowCompleted: WorkflowCompleted? = null,
    val unknownWorkflowWaited: UnknownWorkflowWaited? = null
) : Envelope<ClientMessage> {
    init {
        val noNull = listOfNotNull(
            taskCompleted,
            unknownTaskWaited,
            workflowCompleted,
            unknownWorkflowWaited
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
            is UnknownTaskWaited -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.UNKNOWN_TASK_WAITED,
                unknownTaskWaited = msg
            )
            is WorkflowCompleted -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_COMPLETED,
                workflowCompleted = msg
            )
            is UnknownWorkflowWaited -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.UNKNOWN_WORKFLOW_WAITED,
                unknownWorkflowWaited = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message(): ClientMessage = when (type) {
        ClientMessageType.TASK_COMPLETED -> taskCompleted!!
        ClientMessageType.UNKNOWN_TASK_WAITED -> unknownTaskWaited!!
        ClientMessageType.WORKFLOW_COMPLETED -> workflowCompleted!!
        ClientMessageType.UNKNOWN_WORKFLOW_WAITED -> unknownWorkflowWaited!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
