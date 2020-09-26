package io.infinitic.tests.tasks.inMemory

import io.infinitic.common.taskManager.data.TaskInstance
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage

internal class InMemoryStorageTest : InMemoryTaskStateStorage() {
    fun isTerminated(task: TaskInstance): Boolean = getTaskEngineState(task.taskId) == null
}
