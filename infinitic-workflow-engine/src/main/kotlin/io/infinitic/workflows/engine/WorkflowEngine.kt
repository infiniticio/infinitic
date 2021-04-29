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

import io.infinitic.common.clients.messages.UnknownWorkflow
import io.infinitic.common.clients.messages.WorkflowAlreadyCompleted
import io.infinitic.common.clients.messages.WorkflowCanceled
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.workflows.data.workflows.WorkflowStatus
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.ChildWorkflowFailed
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.interfaces.MethodRunMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.RemoveWorkflowTag
import io.infinitic.workflows.engine.handlers.cancelWorkflow
import io.infinitic.workflows.engine.handlers.childWorkflowCanceled
import io.infinitic.workflows.engine.handlers.childWorkflowCompleted
import io.infinitic.workflows.engine.handlers.childWorkflowFailed
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.retryWorkflowTask
import io.infinitic.workflows.engine.handlers.sendToChannel
import io.infinitic.workflows.engine.handlers.taskCanceled
import io.infinitic.workflows.engine.handlers.taskCompleted
import io.infinitic.workflows.engine.handlers.taskFailed
import io.infinitic.workflows.engine.handlers.timerCompleted
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import kotlin.coroutines.coroutineContext

class WorkflowEngine(
    storage: WorkflowStateStorage,
    sendEventsToClient: SendToClient,
    sendToWorkflowTagEngine: SendToWorkflowTagEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToWorkflowEngineAfter: SendToWorkflowEngineAfter
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val storage = LoggedWorkflowStateStorage(storage)

    private val output = WorkflowEngineOutput(
        sendEventsToClient,
        sendToWorkflowTagEngine,
        sendToTaskEngine,
        sendToWorkflowEngine,
        sendToWorkflowEngineAfter
    )

    suspend fun handle(message: WorkflowEngineMessage) {
        val state = process(message) ?: return

        storage.putState(message.workflowId, state)

        // delete state if terminated
        // the delay makes tests easier, avoiding failure of synchronous requests
        if (state.workflowStatus == WorkflowStatus.TERMINATED) {
            CoroutineScope(coroutineContext).launch {
                delay(200L)
                storage.delState(message.workflowId)
            }
        }
    }

    private suspend fun process(message: WorkflowEngineMessage): WorkflowState? {
        logger.warn("receiving {}", message)

        // get associated state
        val state = storage.getState(message.workflowId)

        // if no state (newly created workflow or terminated workflow)
        if (state == null) {
            if (message is DispatchWorkflow) {
                return dispatchWorkflow(output, message)
            }

            if (message is WaitWorkflow) {
                val unknownWorkflow = UnknownWorkflow(message.clientName, message.workflowId)
                output.sendEventsToClient(unknownWorkflow)
            }
            // discard all other messages if workflow is already terminated
            logDiscardingMessage(message, "for having null state")

            return null
        }

        // check if this message has already been handled
        if (state.lastMessageId == message.messageId) {
            logDiscardingMessage(message, "as state already contains this messageId")

            return null
        }

        // discard all message (except client request is already terminated)
        if (state.workflowStatus == WorkflowStatus.TERMINATED && message !is WaitWorkflow) {
            logDiscardingMessage(message, "as workflow is already terminated")

            return null
        }

        // check is this workflow has already been launched
        // (a DispatchWorkflow (child) can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message is DispatchWorkflow) {
            logDiscardingMessage(message, "as workflow has already been launched")

            return null
        }

        // check is this workflowTask is the current one
        // (a workflowTask can be dispatched twice if the engine is shutdown while processing a workflowTask)
        if (message.isWorkflowTask() &&
            message is TaskCompleted &&
            message.taskId != state.runningWorkflowTaskId
        ) {
            logDiscardingMessage(message, "as workflowTask is not the current one")

            return null
        }

        // set current messageId
        state.lastMessageId = message.messageId

        // if a workflow task is ongoing then buffer this message, except for WorkflowTaskCompleted of course
        // except also for WaitWorkflow, as we want to handle it asap to avoid terminating the workflow before it
        if (state.runningWorkflowTaskId != null &&
            message !is WaitWorkflow &&
            ! message.isWorkflowTask()
        ) {
            // buffer this message
            state.bufferedMessages.add(message)

            return state
        }

        // process this message
        processMessage(state, message)

        // process all buffered messages
        while (
            state.workflowStatus == WorkflowStatus.ALIVE &&
            state.runningWorkflowTaskId == null && // if a workflowTask is not ongoing
            state.bufferedMessages.size > 0 // if there is at least one buffered message
        ) {
            val bufferedMsg = state.bufferedMessages.removeAt(0)
            logger.debug("workflowId {} - processing buffered message {}", bufferedMsg.workflowId, bufferedMsg)
            processMessage(state, bufferedMsg)
        }

        return state
    }

    private fun logDiscardingMessage(message: WorkflowEngineMessage, reason: String) {
        logger.info("workflowId {} - discarding {}: {}", message.workflowId, reason, message)
    }

    private suspend fun processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
        // if message is related to a workflowTask, it's not running anymore
        if (message.isWorkflowTask()) state.runningWorkflowTaskId = null

        // if methodRun has already been cleaned (completed), then discard the message
        if (message is MethodRunMessage && state.getMethodRun(message.methodRunId) == null) {
            logDiscardingMessage(message, "as null methodRun")

            return
        }

        val o = when (message) {
            is DispatchWorkflow -> throw RuntimeException("DispatchWorkflow should not reach this point")
            is CancelWorkflow -> cancelWorkflow(output, state)
            is SendToChannel -> sendToChannel(output, state, message)
            is WaitWorkflow -> waitWorkflow(output, state, message)
            is CompleteWorkflow -> TODO()
            is RetryWorkflowTask -> retryWorkflowTask(output, state)
            is ChildWorkflowFailed -> childWorkflowFailed(output, state, message)
            is ChildWorkflowCanceled -> childWorkflowCanceled(output, state, message)
            is ChildWorkflowCompleted -> childWorkflowCompleted(output, state, message)
            is TimerCompleted -> timerCompleted(output, state, message)
            is TaskFailed -> taskFailed(output, state, message)
            is TaskCanceled -> taskCanceled(output, state, message)
            is TaskCompleted -> taskCompleted(output, state, message)
        }

        // workflow is terminated if all methodRuns have been deleted
        if (state.methodRuns.size == 0) {
            state.workflowStatus = WorkflowStatus.TERMINATED
        }

        when (state.workflowStatus) {
            WorkflowStatus.ALIVE -> Unit
            WorkflowStatus.TERMINATED -> {
                // remove tags reference to this instance
                removeTags(output, state)
            }
            WorkflowStatus.CANCELED -> {
                // send cancellation info to waiting clients
                state.methodRuns.forEach { methodRun ->
                    methodRun.waitingClients.forEach {
                        val workflowCanceled = WorkflowCanceled(
                            clientName = it,
                            workflowId = state.workflowId,
                        )
                        output.sendEventsToClient(workflowCanceled)
                    }
                    // remove tags reference to this instance
                    removeTags(output, state)
                }
            }
        }
    }

    private suspend fun removeTags(output: WorkflowEngineOutput, state: WorkflowState) {
        state.workflowTags.map {
            val removeWorkflowTag = RemoveWorkflowTag(
                workflowTag = it,
                workflowName = state.workflowName,
                workflowId = state.workflowId,
            )
            output.sendToWorkflowTagEngine(removeWorkflowTag)
        }
    }

    private suspend fun waitWorkflow(output: WorkflowEngineOutput, state: WorkflowState, msg: WaitWorkflow) {
        when (val main = state.methodRuns.find { it.methodRunId.id == msg.workflowId.id }) {
            null -> {
                val workflowAlreadyCompleted = WorkflowAlreadyCompleted(msg.clientName, msg.workflowId)
                output.sendEventsToClient(workflowAlreadyCompleted)
            }
            else -> main.waitingClients.add(msg.clientName)
        }
    }
}
