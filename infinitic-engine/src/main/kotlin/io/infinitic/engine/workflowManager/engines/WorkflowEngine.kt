// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.workflowManager.engines

import io.infinitic.common.data.interfaces.inc
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.workflows.data.states.WorkflowState
import io.infinitic.engine.workflowManager.storages.WorkflowStateStorage
import io.infinitic.common.workflows.messages.CancelWorkflow
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.common.workflows.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.messages.TimerCompleted
import io.infinitic.common.workflows.messages.ObjectReceived
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.common.workflows.messages.TaskCanceled
import io.infinitic.common.workflows.messages.TaskCompleted
import io.infinitic.common.workflows.messages.TaskDispatched
import io.infinitic.common.workflows.messages.WorkflowCanceled
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.engine.workflowManager.engines.handlers.ChildWorkflowCompletedHandler
import io.infinitic.engine.workflowManager.engines.handlers.DispatchWorkflowHandler
import io.infinitic.engine.workflowManager.engines.handlers.TaskCompletedHandler
import io.infinitic.engine.workflowManager.engines.handlers.WorkflowTaskCompletedHandler

class WorkflowEngine(
    private val storage: WorkflowStateStorage,
    private val dispatcher: Dispatcher
) {

    companion object {
        const val META_WORKFLOW_ID = "workflowId"
        const val META_METHOD_RUN_ID = "methodRunId"
    }

    suspend fun handle(msg: ForWorkflowEngineMessage) {
        // immediately discard irrelevant messages
        when (msg) {
            is WorkflowTaskDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
            else -> Unit
        }

        // get associated state
        var state = storage.getState(msg.workflowId)

        // if no state (can happen for a newly created workflow or a terminated workflow)
        if (state == null) {
            if (msg is DispatchWorkflow) {
                state = dispatchWorkflow(msg)
                storage.createState(msg.workflowId, state)
            }
            // discard all other types of message as its workflow is already terminated
            return
        }

        // if a workflow task is ongoing then buffer this message (except for WorkflowTaskCompleted)
        if (state.currentWorkflowTaskId != null && msg !is WorkflowTaskCompleted) {
            // buffer this message
            state.bufferedMessages.add(msg)
            // update state
            storage.updateState(msg.workflowId, state)

            return
        }

        // process this message
        processMessage(state, msg)
        // process all buffered messages
        while (
            state.currentMethodRuns.size > 0 && // if workflow is not terminated
            state.currentWorkflowTaskId == null && // if a workflowTask is not ongoing
            state.bufferedMessages.size > 0 // if there is at least one buffered message
        ) {
            val bufferedMsg = state.bufferedMessages.removeAt(0)
            processMessage(state, bufferedMsg)
        }

        // update state
        if (state.currentMethodRuns.size == 0) {
            storage.deleteState(msg.workflowId)
        } else {
            storage.updateState(msg.workflowId, state)
        }
    }

    private suspend fun processMessage(state: WorkflowState, msg: ForWorkflowEngineMessage) {
        when (msg) {
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
    }

    private suspend fun dispatchWorkflow(msg: DispatchWorkflow) =
        DispatchWorkflowHandler(dispatcher).handle(msg)

    private suspend fun workflowTaskCompleted(state: WorkflowState, msg: WorkflowTaskCompleted) =
        WorkflowTaskCompletedHandler(dispatcher).handle(state, msg)

    private suspend fun taskCompleted(state: WorkflowState, msg: TaskCompleted) =
        TaskCompletedHandler(dispatcher).handle(state, msg)

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

    private suspend fun childWorkflowCompleted(state: WorkflowState, msg: ChildWorkflowCompleted) =
        ChildWorkflowCompletedHandler(dispatcher).handle(state, msg)

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
