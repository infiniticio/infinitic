package io.infinitic.taskManager.tests.inMemory

import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.engine.storage.InMemoryTaskStateStorage
import io.infinitic.taskManager.engine.storage.TaskStateStorage
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.engine.storages.InMemoryWorkflowStateStorage
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class InMemoryStorageTest(
    val workflowStateStorage: InMemoryWorkflowStateStorage = InMemoryWorkflowStateStorage(),
    val taskStateStorage: InMemoryTaskStateStorage = InMemoryTaskStateStorage()
) : WorkflowStateStorage by workflowStateStorage, TaskStateStorage by taskStateStorage {
    fun isTerminated(task: TaskInstance): Boolean = taskStateStorage.getTaskEngineState(task.taskId) == null
    fun isTerminated(workflowInstance: WorkflowInstance): Boolean = workflowStateStorage.getState(workflowInstance.workflowId) == null

    fun reset() {
        workflowStateStorage.reset()
        taskStateStorage.reset()
    }
}
