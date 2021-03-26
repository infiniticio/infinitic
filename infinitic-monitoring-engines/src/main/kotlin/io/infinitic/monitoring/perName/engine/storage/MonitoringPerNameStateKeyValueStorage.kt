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

package io.infinitic.monitoring.perName.engine.storage

import io.infinitic.common.monitoring.perName.state.MonitoringPerNameState
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.data.TaskName

/**
 * This MonitoringPerNameStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class MonitoringPerNameStateKeyValueStorage(
    protected val storage: KeyValueStorage,
    private val cache: KeyValueCache<MonitoringPerNameState>
) : MonitoringPerNameStateStorage {

    override val getStateFn: GetMonitoringPerNameState = { taskName: TaskName ->
        val key = getMonitoringPerNameStateKey(taskName)
        cache.getValue(key) ?: run {
            logger.debug("taskName {} - getStateFn - absent from cache, get from storage", taskName)
            storage.getValue(key)?.let { MonitoringPerNameState.fromByteArray(it) }
        }
    }

    override val putStateFn: PutMonitoringPerNameState = {
        taskName: TaskName,
        state: MonitoringPerNameState,
        ->
        val key = getMonitoringPerNameStateKey(taskName)
        cache.putValue(key, state)
        storage.putValue(key, state.toByteArray())
    }

    override val delStateFn: DelMonitoringPerNameState = { taskName: TaskName ->
        val key = getMonitoringPerNameStateKey(taskName)
        cache.delValue(key)
        storage.delValue(key)
    }

    /*
    Use for tests
     */
    fun flush() {
        if (storage is Flushable) {
            storage.flush()
        } else {
            throw Exception("Storage non flushable")
        }
        if (cache is Flushable) {
            cache.flush()
        } else {
            throw Exception("Cache non flushable")
        }
    }

    internal fun getMonitoringPerNameStateKey(taskName: TaskName) = "monitoringPerName.state.$taskName"
}
