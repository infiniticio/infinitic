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
    val addTagToWorkflow: AddTagToWorkflow? = null,
    val removeTagFromWorkflow: RemoveTagFromWorkflow? = null,
    val sendSignalByTag: SendSignalByTag? = null,
    val cancelWorkflowByTag: CancelWorkflowByTag? = null,
    val retryWorkflowTaskByTag: RetryWorkflowTaskByTag? = null,
    val dispatchMethodByTag: DispatchMethodByTag? = null,
    val getWorkflowIdsByTag: GetWorkflowIdsByTag? = null,
) : Envelope<WorkflowTagEngineMessage> {

    init {
        val noNull = listOfNotNull(
            addTagToWorkflow,
            removeTagFromWorkflow,
            sendSignalByTag,
            cancelWorkflowByTag,
            retryWorkflowTaskByTag,
            dispatchMethodByTag,
            getWorkflowIdsByTag
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require("${noNull.first().workflowName}" == name)
    }

    companion object {
        fun from(msg: WorkflowTagEngineMessage) = when (msg) {
            is AddTagToWorkflow -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.ADD_TAG_TO_WORKFLOW,
                addTagToWorkflow = msg
            )
            is RemoveTagFromWorkflow -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.REMOVE_TAG_FROM_WORKFLOW,
                removeTagFromWorkflow = msg
            )
            is SendSignalByTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.SEND_SIGNAL_BY_TAG,
                sendSignalByTag = msg
            )
            is CancelWorkflowByTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.CANCEL_WORKFLOW_BY_TAG,
                cancelWorkflowByTag = msg
            )
            is RetryWorkflowTaskByTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.RETRY_WORKFLOW_TASK_BY_TAG,
                retryWorkflowTaskByTag = msg
            )
            is DispatchMethodByTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.DISPATCH_METHOD_BY_TAG,
                dispatchMethodByTag = msg
            )
            is GetWorkflowIdsByTag -> WorkflowTagEngineEnvelope(
                "${msg.workflowName}",
                WorkflowTagEngineMessageType.GET_WORKFLOW_IDS_BY_TAG,
                getWorkflowIdsByTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        WorkflowTagEngineMessageType.ADD_TAG_TO_WORKFLOW -> addTagToWorkflow!!
        WorkflowTagEngineMessageType.REMOVE_TAG_FROM_WORKFLOW -> removeTagFromWorkflow!!
        WorkflowTagEngineMessageType.SEND_SIGNAL_BY_TAG -> sendSignalByTag!!
        WorkflowTagEngineMessageType.CANCEL_WORKFLOW_BY_TAG -> cancelWorkflowByTag!!
        WorkflowTagEngineMessageType.RETRY_WORKFLOW_TASK_BY_TAG -> retryWorkflowTaskByTag!!
        WorkflowTagEngineMessageType.DISPATCH_METHOD_BY_TAG -> dispatchMethodByTag!!
        WorkflowTagEngineMessageType.GET_WORKFLOW_IDS_BY_TAG -> getWorkflowIdsByTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
