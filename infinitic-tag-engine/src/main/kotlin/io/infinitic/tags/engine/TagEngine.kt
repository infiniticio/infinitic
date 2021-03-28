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

import io.infinitic.common.clients.messages.SendToChannelFailed
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.AddWorkflowTag
import io.infinitic.common.tags.messages.CancelTaskPerTag
import io.infinitic.common.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.tags.messages.RemoveTaskTag
import io.infinitic.common.tags.messages.RemoveWorkflowTag
import io.infinitic.common.tags.messages.RetryTaskPerTag
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.tags.engine.output.LoggedTagEngineOutput
import io.infinitic.tags.engine.output.TagEngineOutput
import io.infinitic.tags.engine.storage.TagStateStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TagEngine(
    val storage: TagStateStorage,
    output: TagEngineOutput
) {
    private val output = LoggedTagEngineOutput(output)

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TagEngineMessage) {
        logger.debug("receiving {} (messageId {})", message, message.messageId)

        when (message) {
            is AddTaskTag -> addTaskTag(message)
            is RemoveTaskTag -> removeTaskTag(message)
            is CancelTaskPerTag -> cancelTaskPerTag(message)
            is RetryTaskPerTag -> retryTaskPerTag(message)
            is AddWorkflowTag -> addWorkflowTag(message)
            is RemoveWorkflowTag -> removeWorkflowTag(message)
            is CancelWorkflowPerTag -> cancelWorkflowPerTag(message)
            is SendToChannelPerTag -> sendToChannelPerTag(message)
        }

        storage.setLastMessageId(message.tag, message.name, message.messageId)
    }

    private suspend fun addTaskTag(message: AddTaskTag) {
        storage.addId(message.tag, message.name, message.taskId.id)
    }

    private suspend fun removeTaskTag(message: RemoveTaskTag) {
        storage.removeId(message.tag, message.name, message.taskId.id)
    }

    private suspend fun retryTaskPerTag(message: RetryTaskPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getIds(message.tag, message.name)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val retryTask = RetryTask(
                    taskId = TaskId(it),
                    taskName = message.name,
                    methodName = message.methodName,
                    methodParameterTypes = message.methodParameterTypes,
                    methodParameters = message.methodParameters,
                    taskMeta = message.taskMeta,
                    taskOptions = message.taskOptions
                )
                output.sendToTaskEngine(retryTask)
            }
        }
    }

    private suspend fun cancelTaskPerTag(message: CancelTaskPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getIds(message.tag, message.name)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val cancelTask = CancelTask(
                    taskId = TaskId(it),
                    taskName = message.name,
                    taskReturnValue = message.taskReturnValue
                )
                output.sendToTaskEngine(cancelTask)
            }
        }
    }

    private suspend fun cancelWorkflowPerTag(message: CancelWorkflowPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getIds(message.tag, message.name)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val cancelWorkflow = CancelWorkflow(
                    workflowId = WorkflowId(it),
                    workflowName = message.name,
                    workflowReturnValue = message.workflowReturnValue
                )
                output.sendToWorkflowEngine(cancelWorkflow)
            }
        }
    }

    private suspend fun sendToChannelPerTag(message: SendToChannelPerTag) {
        // sending to channel is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getIds(message.tag, message.name)
        when (ids.isEmpty()) {
            true -> {
                if (message.clientWaiting) {
                    val sendToChannelFailed = SendToChannelFailed(
                        message.clientName,
                        message.channelEventId
                    )
                    output.sendToClientResponse(sendToChannelFailed)
                }
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val sendToChannel = SendToChannel(
                    clientName = message.clientName,
                    clientWaiting = message.clientWaiting,
                    workflowId = WorkflowId(it),
                    workflowName = message.name,
                    channelEventId = message.channelEventId,
                    channelName = message.channelName,
                    channelEvent = message.channelEvent,
                    channelEventTypes = message.channelEventTypes
                )
                output.sendToWorkflowEngine(sendToChannel)
            }
        }
    }

    private suspend fun addWorkflowTag(message: AddWorkflowTag) {
        storage.addId(message.tag, message.name, message.workflowId.id)
    }

    private suspend fun removeWorkflowTag(message: RemoveWorkflowTag) {
        storage.removeId(message.tag, message.name, message.workflowId.id)
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: TagEngineMessage) =
        when (storage.getLastMessageId(message.tag, message.name)) {
            message.messageId -> {
                logger.info("discarding as state already contains this messageId: {}", message)
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: TagEngineMessage) {
        logger.debug("discarding {} as no id found for the provided tag", message)
    }
}
