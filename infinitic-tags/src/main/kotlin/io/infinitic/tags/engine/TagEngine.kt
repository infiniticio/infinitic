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
import io.infinitic.common.tags.state.TagState
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

        // get state
        val state = storage
            .getState(message.tag)
            ?: TagState(message.tag, null, mutableSetOf())

        if (state.lastMessageId == message.messageId) {
            // this message has already been handled
            logger.info("discarding as state already contains this messageId: {}", message)

            return
        }

        when (message) {
            is SendToChannel -> sendToChannel(state, message)
            is WorkflowStarted -> workflowStarted(state, message)
            is WorkflowTerminated -> workflowTerminated(state, message)
        }
    }

    private suspend fun sendToChannel(state: TagState, message: SendToChannel) {
        when (state.workflowIds.isEmpty()) {
            true -> logger.debug("discarding as state does not contain any workflowIds: {}", message)
            false -> state.workflowIds.forEach {
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

                tagEngineOutput.sendToWorkflowEngine(state, msg)
            }
        }
    }

    private suspend fun workflowStarted(state: TagState, message: WorkflowStarted) {
        state.workflowIds.add(message.workflowId)
        storage.puState(state.tag, state)
    }

    private suspend fun workflowTerminated(state: TagState, message: WorkflowTerminated) {
        state.workflowIds.remove(message.workflowId)
        when (state.workflowIds.isEmpty()) {
            true -> storage.delState(state.tag)
            false -> storage.puState(state.tag, state)
        }
    }
}
