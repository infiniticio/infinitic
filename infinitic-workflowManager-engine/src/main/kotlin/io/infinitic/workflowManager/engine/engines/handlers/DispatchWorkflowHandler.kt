package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.messages.DecisionDispatched
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage

class DispatchWorkflowHandler  (
    private val storage: WorkflowStateStorage,
    private val dispatcher: Dispatcher
) : MsgHandler() {
    suspend fun handle(msg: DispatchWorkflow) {
        // defines method to run
        val methodRun = MethodRun(
            parentWorkflowId = msg.parentWorkflowId,
            parentMethodRunId = msg.parentMethodRunId,
            methodName = msg.methodName,
            methodInput = msg.methodInput
        )

        // defines workflow task input
        val workflowTaskInput = WorkflowTaskInput(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            workflowPropertyStore = PropertyStore(), // filterStore(state.propertyStore, listOf(methodRun))
            workflowEventIndex = WorkflowEventIndex(0),
            methodRun = methodRun
        )

        // defines workflow task
        val workflowTaskId = WorkflowTaskId()

        val workflowTask = DispatchTask(
            taskId = TaskId("$workflowTaskId"),
            taskName = TaskName("${msg.workflowName}"),
            taskInput = TaskInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().with(WorkflowEngine.META_WORKFLOW_ID, "${msg.workflowId}")
        )

        // dispatch workflow task
        dispatcher.toDeciders(workflowTask)

        // log event
        dispatcher.toWorkflowEngine(
            DecisionDispatched(
                workflowTaskId = workflowTaskId,
                workflowId = msg.workflowId,
                workflowName = msg.workflowName,
                workflowTaskInput = workflowTaskInput
            )
        )

        // initialize state
        val state = WorkflowState(
            workflowId = msg.workflowId,
            currentWorkflowTaskId = workflowTaskId,
            currentMethodRuns = mutableListOf(methodRun)
        )

        storage.createState(msg.workflowId, state)
    }
}
