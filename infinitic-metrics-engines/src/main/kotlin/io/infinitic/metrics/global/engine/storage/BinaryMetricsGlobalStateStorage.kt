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
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.storage.keyValue.WrappedKeyValueStorage
import org.jetbrains.annotations.TestOnly

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class BinaryMetricsGlobalStateStorage(
    storage: KeyValueStorage,
) : MetricsGlobalStateStorage {

    private val storage = WrappedKeyValueStorage(storage)

    override suspend fun getState(): MetricsGlobalState? {
        val key = getMetricsGlobalStateKey()
        return storage.get(key)
            ?.let { MetricsGlobalState.fromByteArray(it) }
    }

    override suspend fun putState(state: MetricsGlobalState) {
        val key = getMetricsGlobalStateKey()
        storage.put(key, state.toByteArray())
    }

    override suspend fun delState() {
        val key = getMetricsGlobalStateKey()
        storage.del(key)
    }

    @TestOnly
    override fun flush() = storage.flush()

    private fun getMetricsGlobalStateKey() = "metricsGlobal.state"
}
