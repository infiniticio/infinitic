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

package io.infinitic.metrics.global.engine.storage

import io.infinitic.common.metrics.global.state.MetricsGlobalState
import mu.KotlinLogging
import org.jetbrains.annotations.TestOnly

class LoggedMetricsGlobalStateStorage(
    val storage: MetricsGlobalStateStorage
) : MetricsGlobalStateStorage {

    private val logger = KotlinLogging.logger {}

    override suspend fun getState(): MetricsGlobalState? {
        val state = storage.getState()
        logger.debug { "getState $state" }

        return state
    }

    override suspend fun putState(state: MetricsGlobalState) {
        logger.debug { "putState $state" }
        storage.putState(state)
    }

    override suspend fun delState() {
        logger.debug("delState")
        storage.delState()
    }

    @TestOnly
    override fun flush() {
        logger.debug("flushing metricsGlobalStateStorage")
        storage.flush()
    }
}
