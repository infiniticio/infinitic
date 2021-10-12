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

package io.infinitic.tags.tasks

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.tags.tasks.storage.LoggedTaskTagStorage
import io.infinitic.tags.tasks.storage.TaskTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class TaskTagEngine(
    val clientName: ClientName,
    storage: TaskTagStorage,
    val sendToTaskEngine: SendToTaskEngine,
    val sendToClient: SendToClient
) {
    private lateinit var scope: CoroutineScope

    private val storage = LoggedTaskTagStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: TaskTagEngineMessage) {
        logger.debug { "receiving $message" }

        process(message)

        storage.setLastMessageId(message.taskTag, message.taskName, message.messageId)
    }

    // coroutineScope let send messages in parallel
    // it's important as we can have a lot of them
    private suspend fun process(message: TaskTagEngineMessage) = coroutineScope {
        scope = this

        when (message) {
            is AddTagToTask -> addTagToTask(message)
            is RemoveTagFromTask -> removeTagFromTask(message)
            is CancelTaskByTag -> cancelTaskByTag(message)
            is RetryTaskByTag -> retryTaskByTag(message)
            is GetTaskIdsByTag -> getTaskIds(message)
        }
    }

    private suspend fun addTagToTask(message: AddTagToTask) {
        storage.addTaskId(message.taskTag, message.taskName, message.taskId)
    }

    private suspend fun removeTagFromTask(message: RemoveTagFromTask) {
        storage.removeTaskId(message.taskTag, message.taskName, message.taskId)
    }

    private suspend fun retryTaskByTag(message: RetryTaskByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val taskIds = storage.getTaskIds(message.taskTag, message.taskName)
        when (taskIds.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> taskIds.forEach {
                val retryTask = RetryTask(
                    taskName = message.taskName,
                    taskId = it,
                    emitterName = clientName
                )
                scope.launch { sendToTaskEngine(retryTask) }
            }
        }
    }

    private suspend fun cancelTaskByTag(message: CancelTaskByTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getTaskIds(message.taskTag, message.taskName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val cancelTask = CancelTask(
                    taskName = message.taskName,
                    taskId = it,
                    emitterName = clientName
                )
                scope.launch { sendToTaskEngine(cancelTask) }
            }
        }
    }

    private suspend fun getTaskIds(message: GetTaskIdsByTag) {
        val taskIds = storage.getTaskIds(message.taskTag, message.taskName)

        val taskIdsByTag = TaskIdsByTag(
            recipientName = message.emitterName,
            message.taskName,
            message.taskTag,
            taskIds,
            emitterName = clientName
        )

        scope.launch { sendToClient(taskIdsByTag) }
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: TaskTagEngineMessage) =
        when (storage.getLastMessageId(message.taskTag, message.taskName)) {
            message.messageId -> {
                logger.info { "discarding as state already contains this messageId: $message" }
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: TaskTagEngineMessage) {
        logger.debug { "discarding as no id found for the provided tag: $message" }
    }
}
