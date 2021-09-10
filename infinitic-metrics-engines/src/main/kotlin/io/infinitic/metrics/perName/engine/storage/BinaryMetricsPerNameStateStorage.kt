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

package io.infinitic.metrics.perName.engine.storage

import io.infinitic.common.data.Name
import io.infinitic.common.metrics.perName.state.MetricsPerNameState
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueStorage

/**
 * This MonitoringPerNameStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class BinaryMetricsPerNameStateStorage(
    private val storage: KeyValueStorage
) : MetricsPerNameStateStorage, Flushable by storage {

    override suspend fun getState(name: Name): MetricsPerNameState? {
        val key = getMetricsPerNameStateKey(name)
        return storage.get(key)
            ?.let { MetricsPerNameState.fromByteArray(it) }
    }
    override suspend fun putState(name: Name, state: MetricsPerNameState) {
        val key = getMetricsPerNameStateKey(name)
        storage.put(key, state.toByteArray())
    }

    override suspend fun delState(name: Name) {
        val key = getMetricsPerNameStateKey(name)
        storage.del(key)
    }

    private fun getMetricsPerNameStateKey(name: Name) = "metricsPerName.state.$name"
}
