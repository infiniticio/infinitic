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

package io.infinitic.tasks.engine.storage.states

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.state.TaskState
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * TaskStateStorage implementations are responsible for storing the different state objects used by the engine.
 */
class LoggedTaskStateStorage(
    val storage: TaskStateStorage
) : TaskStateStorage {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getState(taskId: TaskId): TaskState? {
        val state = storage.getState(taskId)
        logger.debug("taskId {} - getState {}", taskId, state)

        return state
    }

    override suspend fun putState(taskId: TaskId, state: TaskState) {
        logger.debug("taskId {} - putState {}", taskId, state)
        storage.putState(taskId, state)
    }

    override suspend fun delState(taskId: TaskId) {
        logger.debug("taskId {} - delState", taskId)
        storage.delState(taskId)
    }
}
