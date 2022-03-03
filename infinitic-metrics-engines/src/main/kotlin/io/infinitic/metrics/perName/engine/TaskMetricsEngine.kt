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

package io.infinitic.metrics.perName.engine

import io.infinitic.common.data.ClientName
import io.infinitic.common.metrics.global.SendToGlobalMetrics
import io.infinitic.common.metrics.global.messages.TaskCreated
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.metrics.messages.TaskStatusUpdated
import io.infinitic.common.tasks.metrics.state.TaskMetricsState
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.metrics.perName.engine.storage.LoggedTaskMetricsStateStorage
import mu.KotlinLogging

class TaskMetricsEngine(
    private val clientName: ClientName,
    storage: TaskMetricsStateStorage,
    val sendToGlobalMetrics: SendToGlobalMetrics
) {
    val storage = LoggedTaskMetricsStateStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: TaskMetricsMessage) {
        logger.debug { "receiving $message" }

        // get state
        val oldState = storage.getState(message.taskName)

        // checks if this message has already just been handled
        if (oldState != null && oldState.lastMessageId == message.messageId) {
            return logDiscardingMessage(message, "as state already contains this messageId")
        }

        val newState = oldState
            ?.copy(lastMessageId = message.messageId)
            ?: TaskMetricsState(message.messageId, message.taskName)

        when (message) {
            is TaskStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // It's a new task type
        if (oldState == null) {
            val taskCreated = TaskCreated(
                taskName = message.taskName,
                emitterName = clientName
            )
            sendToGlobalMetrics(taskCreated)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.putState(message.taskName, newState)
        }
    }

    private fun handleTaskStatusUpdated(message: TaskStatusUpdated, state: TaskMetricsState) {
        when (message.oldStatus) {
            TaskStatus.RUNNING_OK -> state.runningOkCount--
            TaskStatus.RUNNING_WARNING -> state.runningWarningCount--
            TaskStatus.RUNNING_ERROR -> state.runningErrorCount--
            TaskStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount--
            TaskStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount--
            else -> Unit
        }

        when (message.newStatus) {
            TaskStatus.RUNNING_OK -> state.runningOkCount++
            TaskStatus.RUNNING_WARNING -> state.runningWarningCount++
            TaskStatus.RUNNING_ERROR -> state.runningErrorCount++
            TaskStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount++
            TaskStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount++
        }
    }

    private fun logDiscardingMessage(message: TaskMetricsMessage, cause: String) {
        logger.info { "$cause - discarding $message" }
    }
}
