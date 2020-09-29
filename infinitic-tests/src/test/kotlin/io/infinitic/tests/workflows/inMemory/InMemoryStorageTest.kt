package io.infinitic.taskManager.tests.inMemory

import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.engine.workflowManager.storages.InMemoryWorkflowStateStorage
import io.infinitic.engine.workflowManager.storages.WorkflowStateStorage

class InMemoryStorageTest(
    val workflowStateStorage: InMemoryWorkflowStateStorage = InMemoryWorkflowStateStorage(),
    val taskStateStorage: InMemoryTaskStateStorage = InMemoryTaskStateStorage()
) : WorkflowStateStorage by workflowStateStorage, TaskStateStorage by taskStateStorage {
    fun isTerminated(taskInstance: TaskInstance): Boolean = taskStateStorage.getTaskEngineState(taskInstance.taskId) == null
    fun isTerminated(workflowInstance: WorkflowInstance): Boolean = workflowStateStorage.getState(workflowInstance.workflowId) == null

    fun reset() {
        workflowStateStorage.reset()
        taskStateStorage.reset()
    }
}
