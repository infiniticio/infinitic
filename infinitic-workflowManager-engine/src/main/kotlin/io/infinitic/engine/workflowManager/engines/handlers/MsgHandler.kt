package io.infinitic.engine.workflowManager.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.methodRuns.MethodRunId
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.messages.WorkflowTaskDispatched
import io.infinitic.common.workflowManager.data.states.WorkflowState
import io.infinitic.engine.workflowManager.engines.WorkflowEngine

abstract class MsgHandler(
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
            taskName = TaskName(WorkflowTask::class.java.name),
            taskInput = TaskInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
                .with<TaskMeta>(WorkflowEngine.META_WORKFLOW_ID, "${state.workflowId}")
                .with<TaskMeta>(WorkflowEngine.META_METHOD_RUN_ID, "${methodRun.methodRunId}")
        )

        // dispatch workflow task
        dispatcher.toTaskEngine(workflowTask)

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

    protected fun cleanMethodRun(methodRun: MethodRun, state: WorkflowState) {
        // if everything is completed in methodRun then filter state
        if (methodRun.methodOutput != null &&
            methodRun.pastCommands.all { it.isTerminated() } &&
            methodRun.pastSteps.all { it.isTerminated() }
        ) {
            // TODO("filter workflow if unused properties")
            state.currentMethodRuns.remove(methodRun)
        }
    }
}
