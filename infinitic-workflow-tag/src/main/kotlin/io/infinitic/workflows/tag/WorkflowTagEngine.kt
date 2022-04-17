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

package io.infinitic.workflows.tag

import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.workflows.tag.storage.LoggedWorkflowTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class WorkflowTagEngine(
    private val clientName: ClientName,
    storage: WorkflowTagStorage,
    val sendToWorkflowTag: SendToWorkflowTag,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToClient: SendToClient

) {
    private lateinit var scope: CoroutineScope

    private val storage = LoggedWorkflowTagStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: WorkflowTagMessage) {
        logger.debug { "receiving $message" }

        process(message)

        storage.setLastMessageId(message.workflowTag, message.workflowName, message.messageId)
    }

    // coroutineScope let send messages in parallel
    // it's important as we can have a lot of them
    private suspend fun process(message: WorkflowTagMessage) = coroutineScope {
        scope = this

        when (message) {
            is DispatchWorkflowByCustomId -> dispatchWorkflowByCustomId(message)
            is DispatchMethodByTag -> dispatchMethodByTag(message)
            is AddTagToWorkflow -> addWorkflowTag(message)
            is RemoveTagFromWorkflow -> removeWorkflowTag(message)
            is SendSignalByTag -> sendToChannelByTag(message)
            is CancelWorkflowByTag -> cancelWorkflowByTag(message)
            is RetryWorkflowTaskByTag -> retryWorkflowTaskByTag(message)
            is RetryTasksByTag -> retryTaskByTag(message)
            is GetWorkflowIdsByTag -> getWorkflowIds(message)
        }
    }

    private suspend fun dispatchWorkflowByCustomId(message: DispatchWorkflowByCustomId) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)

        when (ids.size) {
            // this workflow instance does not exist yet
            0 -> {
                // provided tags
                message.workflowTags.map {
                    val addTagToWorkflow = AddTagToWorkflow(
                        workflowName = message.workflowName,
                        workflowTag = it,
                        workflowId = message.workflowId,
                        emitterName = clientName
                    )

                    when (it) {
                        message.workflowTag -> addWorkflowTag(addTagToWorkflow)
                        else -> scope.launch { sendToWorkflowTag(addTagToWorkflow) }
                    }
                }
                // dispatch workflow message
                val dispatchWorkflow = DispatchWorkflow(
                    workflowName = message.workflowName,
                    workflowId = message.workflowId,
                    methodName = message.methodName,
                    methodParameters = message.methodParameters,
                    methodParameterTypes = message.methodParameterTypes,
                    workflowOptions = message.workflowOptions,
                    workflowTags = message.workflowTags,
                    workflowMeta = message.workflowMeta,
                    parentWorkflowName = message.parentWorkflowName,
                    parentWorkflowId = message.parentWorkflowId,
                    parentMethodRunId = message.parentMethodRunId,
                    clientWaiting = message.clientWaiting,
                    emitterName = message.emitterName
                )

                scope.launch { sendToWorkflowEngine(dispatchWorkflow) }
            }
            // Another running workflow instance exist with same custom id
            1 -> {
                logger.debug { "A workflow '${message.workflowName}(${ids.first()})' already exists with tag '${message.workflowTag}'" }

                if (message.clientWaiting) {
                    val waitWorkflow = WaitWorkflow(
                        workflowName = message.workflowName,
                        workflowId = ids.first(),
                        methodRunId = MethodRunId.from(ids.first()),
                        emitterName = message.emitterName,
                    )

                    scope.launch { sendToWorkflowEngine(waitWorkflow) }
                }
            }
            // multiple running workflow instance exist with same custom id
            else -> thisShouldNotHappen(
                "Workflow '${message.workflowName}' with customId '${message.workflowTag}' has multiple ids: ${ids.joinToString()}"
            )
        }
    }

    private suspend fun dispatchMethodByTag(message: DispatchMethodByTag) {
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

    private suspend fun retryWorkflowTaskByTag(message: RetryWorkflowTaskByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val retryWorkflowTask = RetryWorkflowTask(
                    workflowName = message.workflowName,
                    workflowId = it,
                    emitterName = clientName
                )
                scope.launch { sendToWorkflowEngine(retryWorkflowTask) }
            }
        }
    }

    private suspend fun retryTaskByTag(message: RetryTasksByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getWorkflowIds(message.workflowTag, message.workflowName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val retryTasks = RetryTasks(
                    workflowName = message.workflowName,
                    workflowId = it,
                    taskId = message.taskId,
                    taskName = message.taskName,
                    taskStatus = message.taskStatus,
                    emitterName = clientName
                )
                scope.launch { sendToWorkflowEngine(retryTasks) }
            }
        }
    }

    private suspend fun cancelWorkflowByTag(message: CancelWorkflowByTag) {
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

    private suspend fun sendToChannelByTag(message: SendSignalByTag) {
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
                        channelTypes = message.channelTypes,
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

    private suspend fun hasMessageAlreadyBeenHandled(message: WorkflowTagMessage) =
        when (storage.getLastMessageId(message.workflowTag, message.workflowName)) {
            message.messageId -> {
                logger.info { "discarding as state already contains this messageId: $message" }
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: WorkflowTagMessage) {
        logger.debug { "discarding as no id found for the provided tag: $message" }
    }
}
