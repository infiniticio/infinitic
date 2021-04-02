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
data class ClientEnvelope(
    val clientName: ClientName,
    val type: ClientMessageType,
    val taskCompleted: TaskCompleted? = null,
    val workflowCompleted: WorkflowCompleted? = null,
) {
    init {
        val noNull = listOfNotNull(
            taskCompleted,
            workflowCompleted
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
            is WorkflowCompleted -> ClientEnvelope(
                msg.clientName,
                ClientMessageType.WORKFLOW_COMPLETED,
                workflowCompleted = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun message(): ClientMessage = when (type) {
        ClientMessageType.TASK_COMPLETED -> taskCompleted!!
        ClientMessageType.WORKFLOW_COMPLETED -> workflowCompleted!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
