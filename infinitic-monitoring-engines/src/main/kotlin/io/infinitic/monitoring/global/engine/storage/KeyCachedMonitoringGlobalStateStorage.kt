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

package io.infinitic.monitoring.global.engine.storage

import io.infinitic.common.monitoring.global.state.MonitoringGlobalState
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class KeyCachedMonitoringGlobalStateStorage(
    private val storage: KeyValueStorage,
    private val cache: KeyValueCache<MonitoringGlobalState>
) : MonitoringGlobalStateStorage {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getState(): MonitoringGlobalState? {
        val key = getMonitoringGlobalStateKey()
        return cache.getValue(key) ?: run {
            logger.debug("getState - absent from cache, get from storage")
            storage.getValue(key)
                ?.let { MonitoringGlobalState.fromByteArray(it) }
                ?.also { cache.putValue(key, it) }
        }
    }

    override suspend fun putState(state: MonitoringGlobalState) {
        val key = getMonitoringGlobalStateKey()
        cache.putValue(key, state)
        storage.putValue(key, state.toByteArray())
    }

    override suspend fun delState() {
        val key = getMonitoringGlobalStateKey()
        cache.delValue(key)
        storage.delValue(key)
    }

    private fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"

    /*
    Used for tests
     */
    fun flush() {
        if (storage is Flushable) {
            storage.flush()
        } else {
            throw RuntimeException("Storage non flushable")
        }
        if (cache is Flushable) {
            cache.flush()
        } else {
            throw Exception("Cache non flushable")
        }
    }
}
