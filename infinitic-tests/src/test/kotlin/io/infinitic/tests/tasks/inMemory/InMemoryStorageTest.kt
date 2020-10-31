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

package io.infinitic.tests.tasks.inMemory

import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.engine.taskManager.storage.AvroKeyValueTaskStateStorage
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.engine.workflowManager.storages.InMemoryWorkflowStateStorage
import io.infinitic.engine.workflowManager.storages.WorkflowStateStorage
import io.infinitic.storage.api.Flushable
import io.infinitic.storage.api.Storage

class InMemoryStorageTest(
    private val storage: Storage,
//    private val taskStateStorage: TaskStateStorage = AvroKeyValueTaskStateStorage(storage)
    private val taskStateStorage: TaskStateStorage = InMemoryTaskStateStorage()
) : TaskStateStorage by taskStateStorage {

    fun isTerminated(taskInstance: TaskInstance): Boolean = taskStateStorage.getTaskEngineState(taskInstance.taskId) == null

    fun reset() {
        if (taskStateStorage is Flushable) {
            taskStateStorage.flush()
        }
    }
}
