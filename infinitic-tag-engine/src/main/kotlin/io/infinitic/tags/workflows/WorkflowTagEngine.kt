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

package io.infinitic.tags.workflows

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.RemoveWorkflowTag
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.tags.workflows.storage.LoggedWorkflowTagStorage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WorkflowTagEngine(
    storage: WorkflowTagStorage,
    sendToWorkflowEngine: SendToWorkflowEngine
) {
    private val storage = LoggedWorkflowTagStorage(storage)

    private val sendToWorkflowEngine: (suspend (WorkflowEngineMessage) -> Unit) =
        { msg: WorkflowEngineMessage -> sendToWorkflowEngine(msg, MillisDuration(0)) }

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: WorkflowTagEngineMessage) {
        logger.debug("receiving {}", message)

        when (message) {
            is AddWorkflowTag -> addWorkflowTag(message)
            is RemoveWorkflowTag -> removeWorkflowTag(message)
            is CancelWorkflowPerTag -> cancelWorkflowPerTag(message)
            is SendToChannelPerTag -> sendToChannelPerTag(message)
        }

        storage.setLastMessageId(message.workflowTag, message.workflowName, message.messageId)
    }

    private suspend fun cancelWorkflowPerTag(message: CancelWorkflowPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val cancelWorkflow = CancelWorkflow(
                    workflowId = it,
                    workflowName = message.workflowName,
                    workflowReturnValue = message.workflowReturnValue
                )
                sendToWorkflowEngine(cancelWorkflow)
            }
        }
    }

    private suspend fun sendToChannelPerTag(message: SendToChannelPerTag) {
        // sending to channel is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> discardTagWithoutIds(message)
            false -> ids.forEach {
                val sendToChannel = SendToChannel(
                    clientName = message.clientName,
                    workflowId = it,
                    workflowName = message.workflowName,
                    channelEventId = message.channelEventId,
                    channelName = message.channelName,
                    channelEvent = message.channelEvent,
                    channelEventTypes = message.channelEventTypes
                )
                sendToWorkflowEngine(sendToChannel)
            }
        }
    }

    private suspend fun addWorkflowTag(message: AddWorkflowTag) {
        storage.addWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
    }

    private suspend fun removeWorkflowTag(message: RemoveWorkflowTag) {
        storage.removeWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: WorkflowTagEngineMessage) =
        when (storage.getLastMessageId(message.workflowTag, message.workflowName)) {
            message.messageId -> {
                logger.info("discarding as state already contains this messageId: {}", message)
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: WorkflowTagEngineMessage) {
        logger.debug("discarding {} as no id found for the provided tag", message)
    }
}
