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

import io.infinitic.common.clients.messages.UnknownWorkflowWaited
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tags.messages.RemoveWorkflowTag
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.workflows.engine.handlers.childWorkflowCompleted
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.sendToChannel
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.timerCompleted
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import org.slf4j.LoggerFactory

class WorkflowEngine(
    storage: WorkflowStateStorage,
    sendEventsToClient: SendToClient,
    sendToTagEngine: SendToTagEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowEngine: SendToWorkflowEngine
) {
    private val output = WorkflowEngineOutput(
        sendEventsToClient,
        sendToTagEngine,
        { msg: TaskEngineMessage -> sendToTaskEngine(msg, MillisDuration(0)) },
        sendToWorkflowEngine
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    val storage = LoggedWorkflowStateStorage(storage)

    suspend fun handle(message: WorkflowEngineMessage) {
        logger.debug("receiving {}", message)

        // get associated state
        var state = storage.getState(message.workflowId)

        // if no state (newly created workflow or terminated workflow)
        if (state == null) {
            if (message is DispatchWorkflow) {
                state = dispatchWorkflow(output, message)
                storage.putState(message.workflowId, state)

                return
            }
            if (message is WaitWorkflow) {
                output.sendEventsToClient(
                    UnknownWorkflowWaited(
                        message.clientName,
                        message.workflowId
                    )
                )
            }
            // discard all other messages if workflow is already terminated
            logDiscardingMessage(message, "for having null state")

            return
        }

        // check if this message has already been handled
        if (state.lastMessageId == message.messageId) {
            logDiscardingMessage(message, "as state already contains this messageId")

            return
        }

        // check is this workflow has already been launched
        // (a DispatchWorkflow (child) can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message is DispatchWorkflow) {
            logDiscardingMessage(message, "as workflow has already been launched")

            return
        }

        // check is this workflowTask is the current one
        // (a workflowTask can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message.isWorkflowTaskCompleted() &&
            (message as TaskCompleted).taskId != state.runningWorkflowTaskId
        ) {
            logDiscardingMessage(message, "as workflowTask is not the current one")

            return
        }

        // set current messageId
        state.lastMessageId = message.messageId

        // if a workflow task is ongoing then buffer this message, except for WorkflowTaskCompleted of course
        // except also for WaitWorkflow, as we want to handle it asap to avoid terminating the workflow before it
        if (state.runningWorkflowTaskId != null &&
            ! message.isWorkflowTaskCompleted() &&
            message !is WaitWorkflow
        ) {
            // buffer this message
            state.bufferedMessages.add(message)
            // update state
            storage.putState(message.workflowId, state)

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
            logger.debug("workflowId {} - processing buffered message {}", bufferedMsg.workflowId, bufferedMsg)
            processMessage(state, bufferedMsg)
        }

        // update state
        when (state.methodRuns.size) {
            // workflow is terminated
            0 -> {
                // remove tags reference to this instance
                state.tags.map {
                    output.sendToTagEngine(
                        RemoveWorkflowTag(
                            tag = it,
                            name = state.workflowName,
                            workflowId = state.workflowId,
                        )
                    )
                }
                // delete workflow state
                storage.delState(message.workflowId)
            }
            else -> {
                storage.putState(message.workflowId, state)
            }
        }
    }

    private fun logDiscardingMessage(message: WorkflowEngineMessage, reason: String) {
        logger.info("workflowId {} - discarding {}: {} (messageId {})", message.workflowId, reason, message, message.messageId)
    }

    private suspend fun processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
        when (message) {
            is CancelWorkflow -> cancelWorkflow(state, message)
            is ChildWorkflowCanceled -> childWorkflowCanceled(state, message)
            is ChildWorkflowCompleted -> childWorkflowCompleted(output, state, message)
            is TimerCompleted -> timerCompleted(output, state, message)
            is TaskCanceled -> taskCanceled(state, message)
            is TaskCompleted -> taskCompleted(output, state, message)
            is SendToChannel -> sendToChannel(output, state, message)
            is WaitWorkflow -> waitWorkflow(state, message)
            else -> throw RuntimeException("Unexpected WorkflowEngineMessage: $message")
        }
    }

    private suspend fun cancelWorkflow(state: WorkflowState, msg: CancelWorkflow) {
        TODO()
    }

    private suspend fun childWorkflowCanceled(state: WorkflowState, msg: ChildWorkflowCanceled) {
        TODO()
    }

    private suspend fun taskCanceled(state: WorkflowState, msg: TaskCanceled) {
        TODO()
    }

    private fun waitWorkflow(state: WorkflowState, msg: WaitWorkflow) {
        state.clientWaiting.add(msg.clientName)
    }
}
