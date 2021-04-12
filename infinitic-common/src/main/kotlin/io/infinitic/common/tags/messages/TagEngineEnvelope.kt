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
import io.infinitic.common.messages.Envelope
import kotlinx.serialization.Serializable

@Serializable
data class TagEngineEnvelope(
    val name: String,
    val type: TagEngineMessageType,
    val addTaskTag: AddTaskTag? = null,
    val removeTaskTag: RemoveTaskTag? = null,
    val cancelTaskPerTag: CancelTaskPerTag? = null,
    val retryTaskPerTag: RetryTaskPerTag? = null,
    val addWorkflowTag: AddWorkflowTag? = null,
    val removeWorkflowTag: RemoveWorkflowTag? = null,
    val sendToChannelPerTag: SendToChannelPerTag? = null,
    val cancelWorkflowPerTag: CancelWorkflowPerTag? = null
) : Envelope<TagEngineMessage> {
    init {
        val noNull = listOfNotNull(
            addTaskTag,
            removeTaskTag,
            cancelTaskPerTag,
            retryTaskPerTag,
            addWorkflowTag,
            removeWorkflowTag,
            sendToChannelPerTag,
            cancelWorkflowPerTag
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().name}" == name)
    }

    companion object {
        fun from(msg: TagEngineMessage) = when (msg) {
            is AddTaskTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.ADD_TASK_TAG,
                addTaskTag = msg
            )
            is RemoveTaskTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.REMOVE_TASK_TAG,
                removeTaskTag = msg
            )
            is CancelTaskPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.CANCEL_TASK_PER_TAG,
                cancelTaskPerTag = msg
            )
            is RetryTaskPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.RETRY_TASK_PER_TAG,
                retryTaskPerTag = msg
            )
            is AddWorkflowTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.ADD_WORKFLOW_TAG,
                addWorkflowTag = msg
            )
            is RemoveWorkflowTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.REMOVE_WORKFLOW_TAG,
                removeWorkflowTag = msg
            )
            is SendToChannelPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.SEND_TO_CHANNEL_PER_TAG,
                sendToChannelPerTag = msg
            )
            is CancelWorkflowPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.CANCEL_WORKFLOW_PER_TAG,
                cancelWorkflowPerTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        TagEngineMessageType.ADD_TASK_TAG -> addTaskTag!!
        TagEngineMessageType.REMOVE_TASK_TAG -> removeTaskTag!!
        TagEngineMessageType.ADD_WORKFLOW_TAG -> addWorkflowTag!!
        TagEngineMessageType.REMOVE_WORKFLOW_TAG -> removeWorkflowTag!!
        TagEngineMessageType.CANCEL_TASK_PER_TAG -> cancelTaskPerTag!!
        TagEngineMessageType.RETRY_TASK_PER_TAG -> retryTaskPerTag!!
        TagEngineMessageType.CANCEL_WORKFLOW_PER_TAG -> cancelWorkflowPerTag!!
        TagEngineMessageType.SEND_TO_CHANNEL_PER_TAG -> sendToChannelPerTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
