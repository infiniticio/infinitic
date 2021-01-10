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

package io.infinitic.monitoring.global.engine

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.messages.TaskCreated
import io.infinitic.common.monitoring.global.state.MonitoringGlobalState
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MonitoringGlobalEngine(
    val storage: MonitoringGlobalStateStorage
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: MonitoringGlobalMessage, messageId: String?) {
        logger.debug("messageId {} - receiving {}", messageId, message)

        // get state
        val oldState = storage.getState()

        // checks if this message has already just been handled
        if (oldState != null && messageId != null && oldState.messageId == messageId) {
            return logDiscardingMessage(message, messageId, "as state already contains this messageId")
        }

        val newState = oldState
            ?.copy(messageId = messageId)
            ?: MonitoringGlobalState(messageId)

        when (message) {
            is TaskCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(newState, oldState)
        }
    }

    private fun logDiscardingMessage(message: MonitoringGlobalMessage, messageId: String?, reason: String) {
        logger.info("messageId {} - discarding {}: {}", messageId, reason, message)
    }

    private fun handleTaskTypeCreated(message: TaskCreated, state: MonitoringGlobalState) {
        state.taskNames.add(message.taskName)
    }
}
