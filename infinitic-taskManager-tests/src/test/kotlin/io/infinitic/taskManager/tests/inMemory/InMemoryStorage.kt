package io.infinitic.taskManager.tests.inMemory

import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.engine.storage.InMemoryTaskStateStorage

internal class InMemoryStorage : InMemoryTaskStateStorage() {
    fun isTerminated(task: TaskInstance): Boolean = getTaskEngineState(task.taskId) == null
}
