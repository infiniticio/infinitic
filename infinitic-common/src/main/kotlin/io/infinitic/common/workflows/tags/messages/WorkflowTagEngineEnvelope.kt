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

package io.infinitic.common.workflows.tags.messages

import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTagEngineEnvelope(
    val name: String,
    val type: WorkflowTagEngineMessageType,
    val addWorkflowTag: AddWorkflowTag? = null,
    val removeWorkflowTag: RemoveWorkflowTag? = null,
    val sendToChannelPerTag: SendToChannelPerTag? = null,
    val cancelWorkflowPerTag: CancelWorkflowPerTag? = null,
    val retryWorkflowTaskPerTag: RetryWorkflowTaskPerTag? = null,
    val getWorkflowIds: GetWorkflowIds? = null,
) : Envelope<WorkflowTagEngineMessage> {
    init {
        val noNull = listOfNotNull(
            addWorkflowTag,
            removeWorkflowTag,
            sendToChannelPerTag,
            cancelWorkflowPerTag,
            retryWorkflowTaskPerTag,
            getWorkflowIds
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().workflowName}" == name)
    }

    companion object {
        fun from(msg: WorkflowTagEngineMessage) = when (msg) {
            is AddWorkflowTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.ADD_WORKFLOW_TAG,
                addWorkflowTag = msg
            )
            is RemoveWorkflowTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.REMOVE_WORKFLOW_TAG,
                removeWorkflowTag = msg
            )
            is SendToChannelPerTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.SEND_TO_CHANNEL_PER_TAG,
                sendToChannelPerTag = msg
            )
            is CancelWorkflowPerTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.CANCEL_WORKFLOW_PER_TAG,
                cancelWorkflowPerTag = msg
            )
            is RetryWorkflowTaskPerTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.RETRY_WORKFLOW_TASK_PER_TAG,
                retryWorkflowTaskPerTag = msg
            )
            is GetWorkflowIds -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.GET_WORKFLOW_IDS,
                getWorkflowIds = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        WorkflowTagEngineMessageType.ADD_WORKFLOW_TAG -> addWorkflowTag!!
        WorkflowTagEngineMessageType.REMOVE_WORKFLOW_TAG -> removeWorkflowTag!!
        WorkflowTagEngineMessageType.SEND_TO_CHANNEL_PER_TAG -> sendToChannelPerTag!!
        WorkflowTagEngineMessageType.CANCEL_WORKFLOW_PER_TAG -> cancelWorkflowPerTag!!
        WorkflowTagEngineMessageType.RETRY_WORKFLOW_TASK_PER_TAG -> retryWorkflowTaskPerTag!!
        WorkflowTagEngineMessageType.GET_WORKFLOW_IDS -> getWorkflowIds!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
