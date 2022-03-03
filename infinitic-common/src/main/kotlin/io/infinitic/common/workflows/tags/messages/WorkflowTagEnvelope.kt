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
data class WorkflowTagEnvelope(
    val name: String,
    val type: WorkflowTagMessageType,
    val addTagToWorkflow: AddTagToWorkflow? = null,
    val removeTagFromWorkflow: RemoveTagFromWorkflow? = null,
    val sendSignalByTag: SendSignalByTag? = null,
    val cancelWorkflowByTag: CancelWorkflowByTag? = null,
    val retryWorkflowTaskByTag: RetryWorkflowTaskByTag? = null,
    val dispatchMethodByTag: DispatchMethodByTag? = null,
    val getWorkflowIdsByTag: GetWorkflowIdsByTag? = null,
) : Envelope<WorkflowTagMessage> {

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
        fun from(msg: WorkflowTagMessage) = when (msg) {
            is AddTagToWorkflow -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.ADD_TAG_TO_WORKFLOW,
                addTagToWorkflow = msg
            )
            is RemoveTagFromWorkflow -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.REMOVE_TAG_FROM_WORKFLOW,
                removeTagFromWorkflow = msg
            )
            is SendSignalByTag -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.SEND_SIGNAL_BY_TAG,
                sendSignalByTag = msg
            )
            is CancelWorkflowByTag -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.CANCEL_WORKFLOW_BY_TAG,
                cancelWorkflowByTag = msg
            )
            is RetryWorkflowTaskByTag -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.RETRY_WORKFLOW_TASK_BY_TAG,
                retryWorkflowTaskByTag = msg
            )
            is DispatchMethodByTag -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.DISPATCH_METHOD_BY_TAG,
                dispatchMethodByTag = msg
            )
            is GetWorkflowIdsByTag -> WorkflowTagEnvelope(
                "${msg.workflowName}",
                WorkflowTagMessageType.GET_WORKFLOW_IDS_BY_TAG,
                getWorkflowIdsByTag = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    override fun message() = when (type) {
        WorkflowTagMessageType.ADD_TAG_TO_WORKFLOW -> addTagToWorkflow!!
        WorkflowTagMessageType.REMOVE_TAG_FROM_WORKFLOW -> removeTagFromWorkflow!!
        WorkflowTagMessageType.SEND_SIGNAL_BY_TAG -> sendSignalByTag!!
        WorkflowTagMessageType.CANCEL_WORKFLOW_BY_TAG -> cancelWorkflowByTag!!
        WorkflowTagMessageType.RETRY_WORKFLOW_TASK_BY_TAG -> retryWorkflowTaskByTag!!
        WorkflowTagMessageType.DISPATCH_METHOD_BY_TAG -> dispatchMethodByTag!!
        WorkflowTagMessageType.GET_WORKFLOW_IDS_BY_TAG -> getWorkflowIdsByTag!!
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
