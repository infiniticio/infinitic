package io.infinitic.engine.workflowManager.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.properties.PropertyStore
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.messages.WorkflowTaskDispatched
import io.infinitic.common.workflowManager.messages.DispatchWorkflow
import io.infinitic.common.workflowManager.data.states.WorkflowState
import io.infinitic.engine.workflowManager.engines.WorkflowEngine

class DispatchWorkflowHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(msg: DispatchWorkflow): WorkflowState {
        // defines method to run
        val methodRun = MethodRun(
            isMain = true,
            parentWorkflowId = msg.parentWorkflowId,
            parentMethodRunId = msg.parentMethodRunId,
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
            workflowMeta = msg.workflowMeta,
            currentWorkflowTaskId = workflowTaskId,
            currentMethodRuns = mutableListOf(methodRun)
        )
    }
}
