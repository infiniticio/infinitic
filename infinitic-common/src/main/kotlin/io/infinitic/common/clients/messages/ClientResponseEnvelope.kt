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
import kotlinx.serialization.Serializable

@Serializable
data class ClientResponseEnvelope(
    val clientName: ClientName,
    val type: ClientResponseMessageType,
    val taskCompleted: TaskCompleted? = null,
    val workflowCompleted: WorkflowCompleted? = null,
    val sendToChannelCompleted: SendToChannelCompleted? = null,
    val sendToChannelFailed: SendToChannelFailed? = null
) {
    init {
        val noNull = listOfNotNull(
            taskCompleted,
            workflowCompleted,
            sendToChannelCompleted,
            sendToChannelFailed
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require(noNull.first().clientName == clientName)
    }

    companion object {
        fun from(msg: ClientResponseMessage) = when (msg) {
            is TaskCompleted -> ClientResponseEnvelope(
                msg.clientName,
                ClientResponseMessageType.TASK_COMPLETED,
                taskCompleted = msg
            )
            is WorkflowCompleted -> ClientResponseEnvelope(
                msg.clientName,
                ClientResponseMessageType.WORKFLOW_COMPLETED,
                workflowCompleted = msg
            )
            is SendToChannelCompleted -> ClientResponseEnvelope(
                msg.clientName,
                ClientResponseMessageType.SEND_TO_CHANNEL_COMPLETED,
                sendToChannelCompleted = msg
            )
            is SendToChannelFailed -> ClientResponseEnvelope(
                msg.clientName,
                ClientResponseMessageType.SEND_TO_CHANNEL_FAILED,
                sendToChannelFailed = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun message(): ClientResponseMessage = when (type) {
        ClientResponseMessageType.TASK_COMPLETED -> taskCompleted!!
        ClientResponseMessageType.WORKFLOW_COMPLETED -> workflowCompleted!!
        ClientResponseMessageType.SEND_TO_CHANNEL_COMPLETED -> sendToChannelCompleted!!
        ClientResponseMessageType.SEND_TO_CHANNEL_FAILED -> sendToChannelFailed!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
