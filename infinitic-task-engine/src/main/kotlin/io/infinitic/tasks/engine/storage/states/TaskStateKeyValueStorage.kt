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

import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.state.TaskState

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class TaskStateKeyValueStorage(
    private val storage: KeyValueStorage,
    private val cache: KeyValueCache<TaskState>
) : TaskStateStorage {

    override val getStateFn: GetTaskState = { taskId: TaskId ->
        val key = getTaskStateKey(taskId)
        cache.get(key) ?: run {
            logger.debug("taskId {} - getStateFn - absent from cache, get from storage", taskId)
            storage.getState(key)?.let { TaskState.fromByteArray(it) }
        }
    }

    override val putStateFn: PutTaskState = { taskId: TaskId, state: TaskState ->
        val key = getTaskStateKey(taskId)
        cache.put(key, state)
        storage.putState(key, state.toByteArray())
    }

    override val delStateFn: DelTaskState = { taskId: TaskId ->
        val key = getTaskStateKey(taskId)
        cache.del(key)
        storage.delState(key)
    }

    /*
    Used for tests
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

    private fun getTaskStateKey(taskId: TaskId) = "task.state.$taskId"
}
