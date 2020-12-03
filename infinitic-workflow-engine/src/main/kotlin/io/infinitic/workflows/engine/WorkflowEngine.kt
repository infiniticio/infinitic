/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workflows.engine

import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.ObjectReceived
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskDispatched
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowCanceled
import io.infinitic.common.workflows.engine.messages.WorkflowCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowTaskCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.InsertWorkflowEvent
import io.infinitic.workflows.engine.handlers.childWorkflowCompleted
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.workflowTaskCompleted
import io.infinitic.workflows.engine.storage.WorkflowStateStorage

class WorkflowEngine(
    private val stateStorage: WorkflowStateStorage,
    private val insertWorkflowEvent: InsertWorkflowEvent,
    private val sendToWorkflowEngine: SendToWorkflowEngine,
    private val sendToTaskEngine: SendToTaskEngine
) {
    suspend fun handle(message: WorkflowEngineMessage) {
        // store event
        insertWorkflowEvent(message)

        // immediately discard irrelevant messages
        when (message) {
            is WorkflowTaskDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
            else -> Unit
        }

        // get associated state
        var state = stateStorage.getState(message.workflowId)

        // if no state (can happen for a newly created workflow or a terminated workflow)
        if (state == null) {
            if (message is DispatchWorkflow) {
                state = dispatchWorkflow(sendToWorkflowEngine, sendToTaskEngine, message)
                stateStorage.createState(message.workflowId, state)
            }
            // discard all other types of message as its workflow is already terminated
            return
        }

        // if a workflow task is ongoing then buffer this message (except for WorkflowTaskCompleted)
        if (state.runningWorkflowTaskId != null && message !is WorkflowTaskCompleted) {
            // buffer this message
            state.bufferedMessages.add(message)
            // update state
            stateStorage.updateState(message.workflowId, state)

            return
        }

        // process this message
        processMessage(state, message)

        // process all buffered messages
        while (
            state.runningWorkflowTaskId == null && // if a workflowTask is not ongoing
            state.methodRuns.size > 0 && // if workflow is not terminated
            state.bufferedMessages.size > 0 // if there is at least one buffered message
        ) {
            val bufferedMsg = state.bufferedMessages.removeAt(0)
            processMessage(state, bufferedMsg)
        }

        // update state
        if (state.methodRuns.size == 0) {
            stateStorage.deleteState(message.workflowId)
        } else {
            stateStorage.updateState(message.workflowId, state)
        }
    }

    private suspend fun processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
        when (message) {
            is CancelWorkflow -> cancelWorkflow(state, message)
            is ChildWorkflowCanceled -> childWorkflowCanceled(state, message)
            is ChildWorkflowCompleted -> childWorkflowCompleted(sendToWorkflowEngine, sendToTaskEngine, state, message)
            is WorkflowTaskCompleted -> workflowTaskCompleted(sendToWorkflowEngine, sendToTaskEngine, state, message)
            is TimerCompleted -> timerCompleted(state, message)
            is ObjectReceived -> objectReceived(state, message)
            is TaskCanceled -> taskCanceled(state, message)
            is TaskCompleted -> taskCompleted(sendToWorkflowEngine, sendToTaskEngine, state, message)
            else -> throw RuntimeException("Unknown ForWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
    }

    private suspend fun cancelWorkflow(state: WorkflowState, msg: CancelWorkflow) {
        TODO()
    }

    private suspend fun childWorkflowCanceled(state: WorkflowState, msg: ChildWorkflowCanceled) {
        TODO()
    }

    private suspend fun timerCompleted(state: WorkflowState, msg: TimerCompleted) {
        TODO()
    }

    private suspend fun taskCanceled(state: WorkflowState, msg: TaskCanceled) {
        TODO()
    }

    private suspend fun objectReceived(state: WorkflowState, msg: ObjectReceived): WorkflowState {
        TODO()
    }
}
