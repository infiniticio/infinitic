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

package io.infinitic.tasks.metrics.storage

import io.infinitic.common.data.Name
import io.infinitic.common.tasks.metrics.state.TaskMetricsState
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggedTaskMetricsStateStorage(
    val storage: TaskMetricsStateStorage
) : TaskMetricsStateStorage {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getState(name: Name): TaskMetricsState? {
        val state = storage.getState(name)
        logger.debug("name {} - getState {}", name, state)

        return state
    }

    override suspend fun putState(name: Name, state: TaskMetricsState) {
        logger.debug("name {} - putState {}", name, state)
        storage.putState(name, state)
    }

    override suspend fun delState(name: Name) {
        logger.debug("name {} - delState", name)
        storage.delState(name)
    }

    @TestOnly
    override fun flush() {
        logger.debug("flushing metricsPerNameStateStorage")
        storage.flush()
    }
}
