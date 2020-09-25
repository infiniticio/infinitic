package io.infinitic.taskManager.tests.inMemory

import io.infinitic.messaging.api.dispatcher.InMemoryDispatcher
import io.infinitic.common.taskManager.data.TaskStatus
import io.infinitic.common.taskManager.messages.TaskCompleted
import io.infinitic.common.taskManager.messages.TaskStatusUpdated
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.worker.Worker
import io.infinitic.client.workflowManager.Client
import io.infinitic.common.workflowManager.data.methodRuns.MethodRunId
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.messages.WorkflowCompleted
import io.infinitic.common.workflowManager.messages.WorkflowTaskCompleted
import io.infinitic.common.workflowManager.messages.TaskCompleted as TaskCompletedInWorkflow
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
    var taskCounter = 0

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
                taskCounter++
                // if (taskCounter % 100 == 0) println(taskCounter)

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
        taskCounter = 0
    }
}
