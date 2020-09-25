package io.infinitic.taskManager.tests.inMemory

import io.infinitic.common.taskManager.data.TaskInstance
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.common.workflowManager.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.engine.storages.InMemoryWorkflowStateStorage
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

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
