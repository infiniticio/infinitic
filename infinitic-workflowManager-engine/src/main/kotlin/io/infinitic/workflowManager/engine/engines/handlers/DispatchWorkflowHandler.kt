package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTask
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.messages.WorkflowTaskDispatched
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.data.states.WorkflowState
import io.infinitic.workflowManager.engine.engines.WorkflowEngine

class DispatchWorkflowHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(msg: DispatchWorkflow): WorkflowState {
        // defines method to run
        val methodRun = MethodRun(
            isMain = true,
            parentWorkflowId = msg.parentWorkflowId,
            methodName = msg.methodName,
            methodInput = msg.methodInput,
            messageIndexAtStart = WorkflowMessageIndex(0)
        )

        // defines workflow task input
        val workflowTaskInput = WorkflowTaskInput(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            workflowPropertyStore = PropertyStore(),
            workflowMessageIndex = WorkflowMessageIndex(0),
            methodRun = methodRun
        )

        // defines workflow task
        val workflowTaskId = WorkflowTaskId()

        val workflowTask = DispatchTask(
            taskId = TaskId("$workflowTaskId"),
            taskName = TaskName(WorkflowTask::class.java.name),
            taskInput = TaskInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
                .with<TaskMeta>(WorkflowEngine.META_WORKFLOW_ID, "${msg.workflowId}")
                .with<TaskMeta>(WorkflowEngine.META_METHOD_RUN_ID, "${methodRun.methodRunId}")
        )

        // dispatch workflow task
        dispatcher.toTaskEngine(workflowTask)

        // log event
        dispatcher.toWorkflowEngine(
            WorkflowTaskDispatched(
                workflowId = msg.workflowId,
                workflowTaskId = workflowTaskId,
                workflowName = msg.workflowName,
                workflowTaskInput = workflowTaskInput
            )
        )

        // initialize state
        return WorkflowState(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            currentWorkflowTaskId = workflowTaskId,
            currentMethodRuns = mutableListOf(methodRun)
        )
    }
}
