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

package io.infinitic.common.tags.messages

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
data class TagEnvelope(
    val workflowName: WorkflowName,
    val type: TagMessageType,
    val sendToChannel: SendToChannel? = null,
    val workflowStarted: WorkflowStarted? = null,
    val workflowTerminated: WorkflowTerminated? = null
) {
    init {
        val noNull = listOfNotNull(
            sendToChannel,
            workflowStarted,
            workflowTerminated
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require(noNull.first().workflowName == workflowName)
    }

    companion object {
        fun from(msg: TagEngineMessage) = when (msg) {
            is SendToChannel -> TagEnvelope(
                msg.workflowName,
                TagMessageType.SEND_TO_CHANNEL,
                sendToChannel = msg
            )
            is WorkflowStarted -> TagEnvelope(
                msg.workflowName,
                TagMessageType.WORKFLOW_STARTED,
                workflowStarted = msg
            )
            is WorkflowTerminated -> TagEnvelope(
                msg.workflowName,
                TagMessageType.WORKFLOW_TERMINATED,
                workflowTerminated = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun message() = when (type) {
        TagMessageType.SEND_TO_CHANNEL -> sendToChannel!!
        TagMessageType.WORKFLOW_STARTED -> workflowStarted!!
        TagMessageType.WORKFLOW_TERMINATED -> workflowTerminated!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
