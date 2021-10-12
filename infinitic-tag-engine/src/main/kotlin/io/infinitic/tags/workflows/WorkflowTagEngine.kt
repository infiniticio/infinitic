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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.tags.workflows.storage.LoggedWorkflowTagStorage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class WorkflowTagEngine(
    private val clientName: ClientName,
    storage: WorkflowTagStorage,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToClient: SendToClient

) {
    private lateinit var scope: CoroutineScope

    private val storage = LoggedWorkflowTagStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: WorkflowTagEngineMessage) {
        logger.debug { "receiving $message" }

        process(message)

        storage.setLastMessageId(message.workflowTag, message.workflowName, message.messageId)
    }

    // coroutineScope let send messages in parallel
    // it's important as we can have a lot of them
    private suspend fun process(message: WorkflowTagEngineMessage) = coroutineScope {
        scope = this

        @Suppress("UNUSED_VARIABLE")
        val o = when (message) {
            is AddTagToWorkflow -> addWorkflowTag(message)
            is RemoveTagFromWorkflow -> removeWorkflowTag(message)
            is SendSignalByTag -> sendToChannelPerTag(message)
            is CancelWorkflowByTag -> cancelWorkflowPerTag(message)
            is RetryWorkflowTaskByTag -> retryWorkflowTaskPerTag(message)
            is DispatchMethodByTag -> dispatchMethodRunPerTag(message)
            is GetWorkflowIdsByTag -> getWorkflowIds(message)
        }
    }

    private suspend fun dispatchMethodRunPerTag(message: DispatchMethodByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                // parent workflow already applied method to self
                if (it != message.parentWorkflowId) {
                    val dispatchMethod = DispatchMethod(
                        workflowName = message.workflowName,
                        workflowId = it,
                        methodRunId = message.methodRunId,
                        methodName = message.methodName,
                        methodParameters = message.methodParameters,
                        methodParameterTypes = message.methodParameterTypes,
                        parentWorkflowId = message.parentWorkflowId,
                        parentWorkflowName = message.parentWorkflowName,
                        parentMethodRunId = message.parentMethodRunId,
                        clientWaiting = false,
                        emitterName = clientName
                    )
                    scope.launch { sendToWorkflowEngine(dispatchMethod) }
                }
            }
        }
    }

    private suspend fun retryWorkflowTaskPerTag(message: RetryWorkflowTaskByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                // parent workflow already applied method to self
                if (it != message.emitterWorkflowId) {
                    val retryWorkflowTask = RetryWorkflowTask(
                        workflowName = message.workflowName,
                        workflowId = it,
                        emitterName = clientName
                    )
                    scope.launch { sendToWorkflowEngine(retryWorkflowTask) }
                }
            }
        }
    }

    private suspend fun cancelWorkflowPerTag(message: CancelWorkflowByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                // parent workflow already applied method to self
                if (it != message.emitterWorkflowId) {
                    val cancelWorkflow = CancelWorkflow(
                        workflowName = message.workflowName,
                        workflowId = it,
                        methodRunId = MethodRunId.from(it),
                        reason = message.reason,
                        emitterName = clientName
                    )
                    scope.launch { sendToWorkflowEngine(cancelWorkflow) }
                }
            }
        }
    }

    private suspend fun sendToChannelPerTag(message: SendSignalByTag) {
        // sending to channel is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> discardTagWithoutIds(message)
            false -> ids.forEach {
                // parent workflow already applied method to self
                if (it != message.emitterWorkflowId) {
                    val sendSignal = SendSignal(
                        workflowName = message.workflowName,
                        workflowId = it,
                        channelName = message.channelName,
                        channelSignalId = message.channelSignalId,
                        channelSignal = message.channelSignal,
                        channelSignalTypes = message.channelSignalTypes,
                        emitterName = clientName
                    )
                    scope.launch { sendToWorkflowEngine(sendSignal) }
                }
            }
        }
    }

    private suspend fun addWorkflowTag(message: AddTagToWorkflow) {
        storage.addWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
    }

    private suspend fun removeWorkflowTag(message: RemoveTagFromWorkflow) {
        storage.removeWorkflowId(message.workflowTag, message.workflowName, message.workflowId)
    }

    private suspend fun getWorkflowIds(message: GetWorkflowIdsByTag) {
        val workflowIds = storage.getWorkflowIds(message.workflowTag, message.workflowName)

        val workflowIdsByTag = WorkflowIdsByTag(
            recipientName = message.emitterName,
            message.workflowName,
            message.workflowTag,
            workflowIds,
            emitterName = clientName
        )
        scope.launch { sendToClient(workflowIdsByTag) }
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: WorkflowTagEngineMessage) =
        when (storage.getLastMessageId(message.workflowTag, message.workflowName)) {
            message.messageId -> {
                logger.info { "discarding as state already contains this messageId: $message" }
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: WorkflowTagEngineMessage) {
        logger.debug { "discarding as no id found for the provided tag: $message" }
    }
}
