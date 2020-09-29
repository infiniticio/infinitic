package io.infinitic.taskManager.tests.inMemory

import io.infinitic.client.Client
import io.infinitic.messaging.api.dispatcher.InMemoryDispatcher
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.worker.Worker
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.engine.workflowManager.engines.ForWorkflowTaskEngine
import io.infinitic.engine.workflowManager.engines.WorkflowEngine

class InMemoryDispatcherTest(storage: InMemoryStorageTest) : InMemoryDispatcher() {
    val client = Client(this)
    val worker = Worker(this)
    val taskEngine = ForWorkflowTaskEngine(storage, this)
    val workflowEngine = WorkflowEngine(storage, this)
    val monitoringPerName = MonitoringPerName(storage, this)
    val monitoringGlobal = MonitoringGlobal(storage)

    var taskStatus: TaskStatus? = null
    var workflowOutput: Any? = null

    init {
        workflowEngineHandle = {
            workflowEngine.handle(it)

            when (it) {
                is WorkflowCompleted -> { workflowOutput = it.workflowOutput.data }
            }
        }
        taskEngineHandle = {
            taskEngine.handle(it)
        }
        monitoringPerNameHandle = {
            monitoringPerName.handle(it)
            when (it) {
                is TaskStatusUpdated -> { taskStatus = it.newStatus }
            }
        }
        monitoringGlobalHandle = { monitoringGlobal.handle(it) }
        workerHandle = {
            worker.handle(it)
        }
    }

    fun reset() {
        taskStatus = null
        workflowOutput = null
    }
}
