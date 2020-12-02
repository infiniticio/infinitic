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
import io.infinitic.workflows.engine.handlers.childWorkflowCompleted
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.workflowTaskCompleted
import io.infinitic.workflows.engine.storage.WorkflowStateStorage

class WorkflowEngine(
    private val storage: WorkflowStateStorage,
    private val sendToWorkflowEngine: SendToWorkflowEngine,
    private val sendToTaskEngine: SendToTaskEngine
) {
    suspend fun handle(msg: WorkflowEngineMessage) {
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
                state = dispatchWorkflow(sendToWorkflowEngine, sendToTaskEngine, msg)
                storage.createState(msg.workflowId, state)
            }
            // discard all other types of message as its workflow is already terminated
            return
        }

        // if a workflow task is ongoing then buffer this message (except for WorkflowTaskCompleted)
        if (state.runningWorkflowTaskId != null && msg !is WorkflowTaskCompleted) {
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
            state.runningWorkflowTaskId == null && // if a workflowTask is not ongoing
            state.methodRuns.size > 0 && // if workflow is not terminated
            state.bufferedMessages.size > 0 // if there is at least one buffered message
        ) {
            val bufferedMsg = state.bufferedMessages.removeAt(0)
            processMessage(state, bufferedMsg)
        }

        // update state
        if (state.methodRuns.size == 0) {
            storage.deleteState(msg.workflowId)
        } else {
            storage.updateState(msg.workflowId, state)
        }
    }

    private suspend fun processMessage(state: WorkflowState, msg: WorkflowEngineMessage) {
        when (msg) {
            is CancelWorkflow -> cancelWorkflow(state, msg)
            is ChildWorkflowCanceled -> childWorkflowCanceled(state, msg)
            is ChildWorkflowCompleted -> childWorkflowCompleted(sendToWorkflowEngine, sendToTaskEngine, state, msg)
            is WorkflowTaskCompleted -> workflowTaskCompleted(sendToWorkflowEngine, sendToTaskEngine, state, msg)
            is TimerCompleted -> timerCompleted(state, msg)
            is ObjectReceived -> objectReceived(state, msg)
            is TaskCanceled -> taskCanceled(state, msg)
            is TaskCompleted -> taskCompleted(sendToWorkflowEngine, sendToTaskEngine, state, msg)
            else -> throw RuntimeException("Unknown ForWorkflowEngineMessage: ${msg::class.qualifiedName}")
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
