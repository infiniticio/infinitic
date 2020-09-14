package io.infinitic.taskManager.tests.inMemory

import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.engine.storage.InMemoryStateStorage

internal class InMemoryStorage : InMemoryStateStorage() {
    fun isTerminated(task: TaskInstance): Boolean = taskEngineStore[task.taskId.id] == null

    fun reset() {
        taskEngineStore = mapOf()
        monitoringPerNameStore = mapOf()
        monitoringGlobalStore = null
    }
}
