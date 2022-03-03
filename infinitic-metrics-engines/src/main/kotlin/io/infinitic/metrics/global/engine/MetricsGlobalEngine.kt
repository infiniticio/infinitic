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

package io.infinitic.metrics.global.engine

import io.infinitic.common.metrics.global.messages.GlobalMetricsMessage
import io.infinitic.common.metrics.global.messages.TaskCreated
import io.infinitic.common.metrics.global.state.GlobalMetricsState
import io.infinitic.common.metrics.global.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.storage.LoggedMetricsGlobalStateStorage
import mu.KotlinLogging

class MetricsGlobalEngine(
    storage: MetricsGlobalStateStorage
) {
    private val storage = LoggedMetricsGlobalStateStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: GlobalMetricsMessage) {
        logger.debug { "receiving $message" }

        // get state
        val oldState = storage.getState()

        // checks if this message has already just been handled
        if (oldState != null && oldState.lastMessageId == message.messageId) {
            return logDiscardingMessage(message, "as state already contains this messageId")
        }

        val newState = oldState
            ?.copy(lastMessageId = message.messageId)
            ?: GlobalMetricsState(lastMessageId = message.messageId)

        when (message) {
            is TaskCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.putState(newState)
        }
    }

    private fun logDiscardingMessage(message: GlobalMetricsMessage, cause: String) {
        logger.info { "discarding $cause: $message" }
    }

    private fun handleTaskTypeCreated(message: TaskCreated, state: GlobalMetricsState) {
        state.taskNames.add(message.taskName)
    }
}
