package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.messages.WorkflowTaskDispatched
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

abstract class MsgHandler(
    open val storage: WorkflowStateStorage,
    open val dispatcher: Dispatcher
) {
    protected fun getMethodRun(state: WorkflowState, methodRunId: MethodRunId) =
        state.currentMethodRuns.first { it.methodRunId == methodRunId }

    protected suspend fun dispatchWorkflowTask(state: WorkflowState, methodRun: MethodRun) {
        // defines workflow task input
        val workflowTaskInput = WorkflowTaskInput(
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            workflowOptions = state.workflowOptions,
            workflowPropertyStore = state.propertyStore, // TODO filterStore(state.propertyStore, listOf(methodRun))
            workflowMessageIndex = state.currentMessageIndex,
            methodRun = methodRun
        )

        // defines workflow task
        val workflowTaskId = WorkflowTaskId()

        val workflowTask = DispatchTask(
            taskId = TaskId("$workflowTaskId"),
            taskName = TaskName("${state.workflowName}"),
            taskInput = TaskInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().with(WorkflowEngine.META_WORKFLOW_ID, "${state.workflowId}")
        )

        // dispatch workflow task
        dispatcher.toDeciders(workflowTask)

        // log event
        dispatcher.toWorkflowEngine(
            WorkflowTaskDispatched(
                workflowTaskId = workflowTaskId,
                workflowId = state.workflowId,
                workflowName = state.workflowName,
                workflowTaskInput = workflowTaskInput
            )
        )

        state.currentWorkflowTaskId = workflowTaskId
    }
}
