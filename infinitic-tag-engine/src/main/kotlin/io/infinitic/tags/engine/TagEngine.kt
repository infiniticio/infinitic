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

package io.infinitic.tags.engine

import io.infinitic.common.tags.messages.SendToChannel
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.messages.WorkflowStarted
import io.infinitic.common.tags.messages.WorkflowTerminated
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.tags.engine.storage.TagStateStorage
import io.infinitic.tags.engine.transport.TagEngineOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.infinitic.common.workflows.engine.messages.SendToChannel as SendToChannelInWorkflowEngine

class TagEngine(
    private val storage: TagStateStorage,
    private val tagEngineOutput: TagEngineOutput
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TagEngineMessage) {
        logger.debug("receiving {} (messageId {})", message, message.messageId)

        when (message) {
            is SendToChannel -> sendToChannel(message)
            is WorkflowStarted -> workflowStarted(message)
            is WorkflowTerminated -> workflowTerminated(message)
        }

        storage.setLastMessageId(message.tag, message.messageId)
    }

    private suspend fun sendToChannel(message: SendToChannel) {
        // sending to channel is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getIds<WorkflowId>(message.tag)
        when (ids.isEmpty()) {
            true -> logger.debug("discarding {} as no id found for the provided tag", message)
            false -> ids.forEach {
                val msg = SendToChannelInWorkflowEngine(
                    clientName = message.clientName,
                    clientWaiting = message.clientWaiting,
                    workflowId = it,
                    workflowName = message.workflowName,
                    channelEventId = message.channelEventId,
                    channelName = message.channelName,
                    channelEvent = message.channelEvent,
                    channelEventTypes = message.channelEventTypes
                )

                tagEngineOutput.sendToWorkflowEngine(message.messageId, message.tag, msg)
            }
        }
    }

    private suspend fun workflowStarted(message: WorkflowStarted) {
        storage.addId(message.tag, message.workflowId)
    }

    private suspend fun workflowTerminated(message: WorkflowTerminated) {
        storage.removeId(message.tag, message.workflowId)
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: TagEngineMessage): Boolean {
        if (storage.getLastMessageId(message.tag) == message.messageId) {
            logger.info("discarding as state already contains this messageId: {}", message)

            return true
        }
        return false
    }
}
