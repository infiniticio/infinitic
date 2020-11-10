// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.tasks.storage

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.states.TaskState
import io.infinitic.storage.api.KeyValueStorage

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class TaskStateKeyValueStorage(protected val storage: KeyValueStorage) : TaskStateStorage {

    override fun getState(taskId: TaskId) = storage
        .getState(getTaskStateKey(taskId))
        ?.let { TaskState.fromByteBuffer(it) }

    override fun updateState(taskId: TaskId, newState: TaskState, oldState: TaskState?) {
        storage.putState(
            getTaskStateKey(taskId),
            newState.toByteBuffer()
        )
    }

    override fun deleteState(taskId: TaskId) {
        storage.deleteState(getTaskStateKey(taskId))
    }

    private fun getTaskStateKey(taskId: TaskId) = "task.state.$taskId"
}
