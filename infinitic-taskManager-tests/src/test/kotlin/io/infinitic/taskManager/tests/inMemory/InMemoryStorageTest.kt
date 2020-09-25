package io.infinitic.taskManager.tests.inMemory

import io.infinitic.common.taskManager.data.TaskInstance
import io.infinitic.taskManager.engine.storage.InMemoryTaskStateStorage

internal class InMemoryStorageTest : InMemoryTaskStateStorage() {
    fun isTerminated(task: TaskInstance): Boolean = getTaskEngineState(task.taskId) == null
}
