package io.infinitic.workflowManager.engine.engines

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.messages.CancelWorkflow
import io.infinitic.workflowManager.common.messages.ChildWorkflowCanceled
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.messages.DecisionDispatched
import io.infinitic.workflowManager.common.messages.TimerCompleted
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.ObjectReceived
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.common.messages.TaskCanceled
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.messages.TaskDispatched
import io.infinitic.workflowManager.common.messages.WorkflowCanceled
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import org.slf4j.Logger

class WorkflowEngine {
    companion object {
        const val META_WORKFLOW_ID = "workflowId"
    }

    lateinit var logger: Logger
    lateinit var storage: WorkflowStateStorage
    lateinit var dispatcher: Dispatcher

    suspend fun handle(msg: ForWorkflowEngineMessage) {
        // discard immediately irrelevant messages
        when (msg) {
            is DecisionDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
            else -> Unit
        }

        // get associated state
        val state = storage.getState(msg.workflowId)

        // discard message it workflow is already terminated
        if (state == null && msg !is DispatchWorkflow) return

        // if a workflow task is ongoing then store message (except DecisionCompleted)
        if (state?.currentWorkflowTaskId != null && msg !is WorkflowTaskCompleted) {
            // buffer this message
            state.bufferedMessages.add(msg)
            // update state
            storage.updateState(msg.workflowId, state)

            return
        }

        if (state == null)
            dispatchWorkflow(msg as DispatchWorkflow)
        else when (msg) {
            is CancelWorkflow -> cancelWorkflow(state, msg)
            is ChildWorkflowCanceled -> childWorkflowCanceled(state, msg)
            is ChildWorkflowCompleted -> childWorkflowCompleted(state, msg)
            is WorkflowTaskCompleted -> WorkflowTaskCompleted().handle(state, msg)
            is TimerCompleted -> timerCompleted(state, msg)
            is ObjectReceived -> objectReceived(state, msg)
            is TaskCanceled -> taskCanceled(state, msg)
            is TaskCompleted -> taskCompleted(state, msg)
            else -> throw RuntimeException("Unknown ForWorkflowEngineMessage: ${msg::class.qualifiedName}")
        }

//        // store state if modified
//        if (newState != oldState) {
//            storage.updateState(msg.workflowId, newState, oldState)
//        }
    }

    private fun cancelWorkflow(state: WorkflowState, msg: CancelWorkflow): WorkflowState {
        TODO()
    }

    private fun childWorkflowCanceled(state: WorkflowState, msg: ChildWorkflowCanceled): WorkflowState {
        TODO()
    }

    private suspend fun dispatchWorkflow(msg: DispatchWorkflow) {

        // defines method to run
        val methodRun = MethodRun(
            methodRunId = MethodRunId(),
            methodName = msg.methodName,
            methodInput = msg.methodInput
        )

        // defines workflow task input
        val workflowTaskInput = WorkflowTaskInput(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            workflowPropertyStore = PropertyStore(), // filterStore(state.propertyStore, listOf(methodRun))
            workflowTaskIndex = WorkflowTaskIndex(0),
            methodRun = methodRun
        )

        // defines workflow task
        val workflowTaskId = WorkflowTaskId()

        val workflowTask = DispatchTask(
            taskId = TaskId("$workflowTaskId"),
            taskName = TaskName("${msg.workflowName}"),
            taskInput = TaskInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().with(META_WORKFLOW_ID, "${msg.workflowId}")
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

    private fun workflowTaskCompleted(state: WorkflowState, msg: WorkflowTaskCompleted): WorkflowState {
        val workflowTaskOutput = msg.workflowTaskOutput
        // add new commands to MethodRun

        // add new steps to MethodRun

        // update current workflow properties

        // is methodRun is completed, filter state

        // apply buffered messages

        // update state
    }

    private fun timerCompleted(state: WorkflowState, msg: TimerCompleted): WorkflowState {
        TODO()
    }

    private fun taskCanceled(state: WorkflowState, msg: TaskCanceled): WorkflowState {
        TODO()
    }

    private fun taskCompleted(state: WorkflowState, msg: TaskCompleted): WorkflowState {
        TODO()
    }

    private fun childWorkflowCompleted(state: WorkflowState, msg: ChildWorkflowCompleted): WorkflowState {
        TODO()
    }

    private fun objectReceived(state: WorkflowState, msg: ObjectReceived): WorkflowState {
        TODO()
    }

    private fun filterStore(store: PropertyStore, branches: List<MethodRun>): PropertyStore {
        // Retrieve properties at step at completion in branches
        val listProperties1 = branches.flatMap {
            b ->
            b.pastSteps.map { it.propertiesAfterCompletion }
        }
        // Retrieve properties when starting in branches
        val listProperties2 = branches.map {
            b ->
            b.propertiesAtStart
        }
        // Retrieve List<PropertyHash?> relevant for branches
        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
        // Keep only relevant keys
        val properties = store.properties.filterKeys { listHashes.contains(it) }.toMutableMap()

        return PropertyStore(properties)
    }
}
