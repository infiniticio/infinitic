package io.infinitic.workflowManager.engine.engines

import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.messages.CancelWorkflow
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.ChildWorkflowCanceled
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.messages.DecisionDispatched
import io.infinitic.workflowManager.common.messages.TimerCompleted
import io.infinitic.workflowManager.common.messages.ObjectReceived
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.common.messages.TaskCanceled
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.messages.TaskDispatched
import io.infinitic.workflowManager.common.messages.WorkflowCanceled
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import io.infinitic.workflowManager.engine.engines.handlers.DispatchWorkflowHandler
import io.infinitic.workflowManager.engine.engines.handlers.TaskCompletedHandler
import io.infinitic.workflowManager.engine.engines.handlers.WorkflowTaskCompletedHandler

class WorkflowEngine(
    private val storage: WorkflowStateStorage,
    private val dispatcher: Dispatcher
) {

    companion object {
        const val META_WORKFLOW_ID = "workflowId"
        const val META_METHOD_RUN_ID = "methodRunId"
    }

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
            is WorkflowTaskCompleted -> workflowTaskCompleted(state, msg)
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

    private suspend fun dispatchWorkflow(msg: DispatchWorkflow) {
        DispatchWorkflowHandler(storage, dispatcher).handle(msg)
    }
    private suspend fun workflowTaskCompleted(state: WorkflowState, msg: WorkflowTaskCompleted) {
        WorkflowTaskCompletedHandler(storage, dispatcher).handle(state, msg)
    }
    private suspend fun taskCompleted(state: WorkflowState, msg: TaskCompleted) {
        TaskCompletedHandler(storage, dispatcher).handle(state, msg)
    }

    private suspend fun cancelWorkflow(state: WorkflowState, msg: CancelWorkflow): WorkflowState {
        TODO()
    }

    private suspend fun childWorkflowCanceled(state: WorkflowState, msg: ChildWorkflowCanceled): WorkflowState {
        TODO()
    }

    private suspend fun timerCompleted(state: WorkflowState, msg: TimerCompleted): WorkflowState {
        TODO()
    }

    private suspend fun taskCanceled(state: WorkflowState, msg: TaskCanceled): WorkflowState {
        TODO()
    }

    private suspend fun childWorkflowCompleted(state: WorkflowState, msg: ChildWorkflowCompleted): WorkflowState {
        TODO()
    }

    private suspend fun objectReceived(state: WorkflowState, msg: ObjectReceived): WorkflowState {
        TODO()
    }

//    private fun filterStore(store: PropertyStore, branches: List<MethodRun>): PropertyStore {
//        // Retrieve properties at step at completion in branches
//        val listProperties1 = branches.flatMap {
//            b ->
//            b.pastSteps.map { it.propertiesAfterCompletion }
//        }
//        // Retrieve properties when starting in branches
//        val listProperties2 = branches.map {
//            b ->
//            b.propertiesAtStart
//        }
//        // Retrieve List<PropertyHash?> relevant for branches
//        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
//        // Keep only relevant keys
//        val properties = store.properties.filterKeys { listHashes.contains(it) }.toMutableMap()
//
//        return PropertyStore(properties)
//    }
}
