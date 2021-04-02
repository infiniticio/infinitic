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

package io.infinitic.monitoring.perName.engine

import io.infinitic.common.metrics.global.messages.TaskCreated
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.metrics.perName.state.MetricsPerNameState
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.monitoring.perName.engine.output.LoggedMonitoringPerNameOutput
import io.infinitic.monitoring.perName.engine.output.MonitoringPerNameOutput
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MonitoringPerNameEngine(
    val storage: MonitoringPerNameStateStorage,
    output: MonitoringPerNameOutput
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    private val output = LoggedMonitoringPerNameOutput(output)

    suspend fun handle(message: MetricsPerNameMessage) {
        logger.debug("receiving {}", message)

        // get state
        val oldState = storage.getState(message.taskName)

        // checks if this message has already just been handled
        if (oldState != null && oldState.lastMessageId == message.messageId) {
            return logDiscardingMessage(message, "as state already contains this messageId")
        }

        val newState = oldState
            ?.copy(lastMessageId = message.messageId)
            ?: MetricsPerNameState(message.messageId, message.taskName)

        when (message) {
            is TaskStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // It's a new task type
        if (oldState == null) {
            val taskCreated = TaskCreated(taskName = message.taskName)
            output.sendToMonitoringGlobal(taskCreated)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.putState(message.taskName, newState)
        }
    }

    private fun handleTaskStatusUpdated(message: TaskStatusUpdated, state: MetricsPerNameState) {
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

    private fun logDiscardingMessage(message: MetricsPerNameMessage, reason: String) {
        logger.info("{} - discarding {}", reason, message)
    }
}
