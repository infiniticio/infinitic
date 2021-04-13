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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.tags.messages.AddTaskTag
import io.infinitic.common.tasks.tags.messages.CancelTaskPerTag
import io.infinitic.common.tasks.tags.messages.RemoveTaskTag
import io.infinitic.common.tasks.tags.messages.RetryTaskPerTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.tags.tasks.storage.LoggedTaskTagStorage
import io.infinitic.tags.tasks.storage.TaskTagStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TaskTagEngine(
    storage: TaskTagStorage,
    sendToTaskEngine: SendToTaskEngine
) {
    private val storage = LoggedTaskTagStorage(storage)

    private val sendToTaskEngine: (suspend (TaskEngineMessage) -> Unit) =
        { msg: TaskEngineMessage -> sendToTaskEngine(msg, MillisDuration(0)) }

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskTagEngineMessage) {
        logger.debug("receiving {}", message)

        when (message) {
            is AddTaskTag -> addTaskTag(message)
            is RemoveTaskTag -> removeTaskTag(message)
            is CancelTaskPerTag -> cancelTaskPerTag(message)
            is RetryTaskPerTag -> retryTaskPerTag(message)
        }

        storage.setLastMessageId(message.taskTag, message.taskName, message.messageId)
    }

    private suspend fun addTaskTag(message: AddTaskTag) {
        storage.addTaskId(message.taskTag, message.taskName, message.taskId)
    }

    private suspend fun removeTaskTag(message: RemoveTaskTag) {
        storage.removeTaskId(message.taskTag, message.taskName, message.taskId)
    }

    private suspend fun retryTaskPerTag(message: RetryTaskPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getTaskIds(message.taskTag, message.taskName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val retryTask = RetryTask(
                    taskId = it,
                    taskName = message.taskName,
                    methodName = message.methodName,
                    methodParameterTypes = message.methodParameterTypes,
                    methodParameters = message.methodParameters,
                    taskTags = message.taskTags,
                    taskMeta = message.taskMeta,
                    taskOptions = message.taskOptions
                )
                sendToTaskEngine(retryTask)
            }
        }
    }

    private suspend fun cancelTaskPerTag(message: CancelTaskPerTag) {
        // is not an idempotent action
        if (hasMessageAlreadyBeenHandled(message)) return

        val ids = storage.getTaskIds(message.taskTag, message.taskName)
        when (ids.isEmpty()) {
            true -> {
                discardTagWithoutIds(message)
            }
            false -> ids.forEach {
                val cancelTask = CancelTask(
                    taskId = it,
                    taskName = message.taskName,
                    taskReturnValue = message.taskReturnValue
                )
                sendToTaskEngine(cancelTask)
            }
        }
    }

    private suspend fun hasMessageAlreadyBeenHandled(message: TaskTagEngineMessage) =
        when (storage.getLastMessageId(message.taskTag, message.taskName)) {
            message.messageId -> {
                logger.info("discarding as state already contains this messageId: {}", message)
                true
            }
            else -> false
        }

    private fun discardTagWithoutIds(message: TaskTagEngineMessage) {
        logger.debug("discarding {} as no id found for the provided tag", message)
    }
}
