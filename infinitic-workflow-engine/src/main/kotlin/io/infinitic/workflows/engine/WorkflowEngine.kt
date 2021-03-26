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

import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.RemoveWorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
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
import io.infinitic.workflows.engine.handlers.sendToChannel
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.timerCompleted
import io.infinitic.workflows.engine.handlers.workflowTaskCompleted
import io.infinitic.workflows.engine.storage.events.WorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import org.slf4j.LoggerFactory

class WorkflowEngine(
    private val workflowStateStorage: WorkflowStateStorage,
    private val workflowEventStorage: WorkflowEventStorage,
    private val workflowEngineOutput: WorkflowEngineOutput
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: WorkflowEngineMessage) {
        logger.debug("workflowId {} - receiving {} (messageId {})", message.workflowId, message, message.messageId)

        // store event
        workflowEventStorage.insertWorkflowEvent(message)

        // immediately discard irrelevant messages
        when (message) {
            is WorkflowTaskDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
            else -> Unit
        }

        // get associated state
        var state = workflowStateStorage.getState(message.workflowId)

        // if no state (newly created workflow or terminated workflow)
        if (state == null) {
            if (message is DispatchWorkflow) {
                state = dispatchWorkflow(workflowEngineOutput, message)
                workflowStateStorage.putState(message.workflowId, state)

                return
            }
            // discard all other messages if workflow is already terminated
            logDiscardingMessage(message, "for having null state")

            return
        }

        // check if this message has already been handled
        if (state.lastMessageId == message.messageId) {
            warnDiscardingMessage(message, "as state already contains this messageId")

            return
        }

        // check is this workflow has already been launched
        // (a DispatchWorkflow (child) can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message is DispatchWorkflow) {
            warnDiscardingMessage(message, "as workflow has already been launched")

            return
        }

        // check is this workflowTask is the current one
        // (a workflowTask can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message is WorkflowTaskCompleted && message.workflowTaskId != state.runningWorkflowTaskId) {
            warnDiscardingMessage(message, "as workflowTask is not the current one")

            return
        }

        // set current messageId
        state.lastMessageId = message.messageId

        // if a workflow task is ongoing then buffer this message (except for WorkflowTaskCompleted)
        if (state.runningWorkflowTaskId != null && message !is WorkflowTaskCompleted) {
            // buffer this message
            state.bufferedMessages.add(message)
            // update state
            workflowStateStorage.putState(message.workflowId, state)

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
            0 -> { // workflow is terminated

                // remove tags reference to this instance
                val tags = state.workflowOptions.tags.map { Tag(it) } + Tag.of(state.workflowId)
                tags.map {
                    workflowEngineOutput.sendToTagEngine(
                        state,
                        RemoveWorkflowTag(
                            tag = it,
                            name = state.workflowName,
                            workflowId = state.workflowId,
                        )
                    )
                }
                // delete workflow state
                workflowStateStorage.delState(message.workflowId)
            }
            else -> {
                workflowStateStorage.putState(message.workflowId, state)
            }
        }
    }

    private fun logDiscardingMessage(message: WorkflowEngineMessage, reason: String) {
        logger.info("workflowId {} - discarding {}: {} (messageId {})", message.workflowId, reason, message, message.messageId)
    }

    private fun warnDiscardingMessage(message: WorkflowEngineMessage, reason: String) {
        logger.warn("workflowId {} - discarding {}: {} (messageId {})", message.workflowId, reason, message, message.messageId)
    }

    private suspend fun processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
        when (message) {
            is CancelWorkflow -> cancelWorkflow(state, message)
            is ChildWorkflowCanceled -> childWorkflowCanceled(state, message)
            is ChildWorkflowCompleted -> childWorkflowCompleted(workflowEngineOutput, state, message)
            is WorkflowTaskCompleted -> workflowTaskCompleted(workflowEngineOutput, state, message)
            is TimerCompleted -> timerCompleted(workflowEngineOutput, state, message)
            is TaskCanceled -> taskCanceled(state, message)
            is TaskCompleted -> taskCompleted(workflowEngineOutput, state, message)
            is SendToChannel -> sendToChannel(workflowEngineOutput, state, message)
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
}
