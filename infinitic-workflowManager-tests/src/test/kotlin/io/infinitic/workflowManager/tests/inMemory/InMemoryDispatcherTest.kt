package io.infinitic.taskManager.tests.inMemory

import io.infinitic.messaging.api.dispatcher.InMemoryDispatcher
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.common.messages.TaskCompleted
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.worker.Worker
import io.infinitic.workflowManager.client.Client
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTask
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.messages.TaskCompleted as TaskCompletedInWorkflow
import io.infinitic.workflowManager.engine.engines.WorkflowEngine

class InMemoryDispatcherTest(storage: InMemoryStorageTest) : InMemoryDispatcher() {
    val client = Client(this)
    val worker = Worker(this)
    val taskEngine = TaskEngine(storage, this)
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

            // dispatch Task and WorkflowTask to workflow engine after completion
            if (it is TaskCompleted) {
                val wid = it.taskMeta[WorkflowEngine.META_WORKFLOW_ID]
                if (wid != null) {
                    val workflowId = WorkflowId("$wid")
                    val msg = when ("${it.taskName}") {
                        WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                            workflowId = workflowId,
                            workflowTaskId = WorkflowTaskId("${it.taskId}"),
                            workflowTaskOutput = it.taskOutput.data as WorkflowTaskOutput
                        )
                        else -> TaskCompletedInWorkflow(
                            workflowId = workflowId,
                            methodRunId = MethodRunId(it.taskMeta[WorkflowEngine.META_METHOD_RUN_ID].toString()),
                            taskId = it.taskId,
                            taskOutput = it.taskOutput
                        )
                    }
                    toWorkflowEngine(msg)
                }
            }
        }
        monitoringPerNameHandle = {
            monitoringPerName.handle(it)
            when (it) {
                is TaskStatusUpdated -> { taskStatus = it.newStatus }
            }
        }
        monitoringGlobalHandle = { monitoringGlobal.handle(it) }
        workerHandle = { worker.handle(it) }
    }

    fun reset() {
        taskStatus = null
        workflowOutput = null
    }
}
