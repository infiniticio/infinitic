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
import kotlinx.serialization.Serializable

@Serializable
data class TagEngineEnvelope(
    val name: String,
    val type: TagEngineMessageType,
    val sendToChannelPerTag: SendToChannelPerTag? = null,
    val workflowStarted: WorkflowStarted? = null,
    val workflowTerminated: WorkflowTerminated? = null,
    val cancelTaskPerTag: CancelTaskPerTag? = null,
    val cancelWorkflowPerTag: CancelWorkflowPerTag? = null,
    val retryTaskPerTag: RetryTaskPerTag? = null
) {
    init {
        val noNull = listOfNotNull(
            sendToChannelPerTag,
            workflowStarted,
            workflowTerminated,
            cancelTaskPerTag,
            cancelWorkflowPerTag,
            retryTaskPerTag
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().name}" == name)
    }

    companion object {
        fun from(msg: TagEngineMessage) = when (msg) {
            is SendToChannelPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.SEND_TO_CHANNEL_PER_TAG,
                sendToChannelPerTag = msg
            )
            is WorkflowStarted -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.WORKFLOW_STARTED,
                workflowStarted = msg
            )
            is WorkflowTerminated -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.WORKFLOW_TERMINATED,
                workflowTerminated = msg
            )
            is CancelTaskPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.CANCEL_TASK_PER_TAG,
                cancelTaskPerTag = msg
            )
            is CancelWorkflowPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.CANCEL_WORKFLOW_PER_TAG,
                cancelWorkflowPerTag = msg
            )
            is RetryTaskPerTag -> TagEngineEnvelope(
                "${msg.name}",
                TagEngineMessageType.RETRY_TASK_PER_TAG,
                retryTaskPerTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun message() = when (type) {
        TagEngineMessageType.SEND_TO_CHANNEL_PER_TAG -> sendToChannelPerTag!!
        TagEngineMessageType.WORKFLOW_STARTED -> workflowStarted!!
        TagEngineMessageType.WORKFLOW_TERMINATED -> workflowTerminated!!
        TagEngineMessageType.CANCEL_TASK_PER_TAG -> cancelTaskPerTag!!
        TagEngineMessageType.RETRY_TASK_PER_TAG -> retryTaskPerTag!!
        TagEngineMessageType.CANCEL_WORKFLOW_PER_TAG -> cancelWorkflowPerTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
