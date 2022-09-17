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

import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.MethodRunUnknown
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.errors.UnknownWorkflowError
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodEvent
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskEvent
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEvent
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.workflows.engine.handlers.cancelWorkflow
import io.infinitic.workflows.engine.handlers.completeTimer
import io.infinitic.workflows.engine.handlers.dispatchMethodRun
import io.infinitic.workflows.engine.handlers.dispatchWorkflow
import io.infinitic.workflows.engine.handlers.retryTasks
import io.infinitic.workflows.engine.handlers.retryWorkflowTask
import io.infinitic.workflows.engine.handlers.sendSignal
import io.infinitic.workflows.engine.handlers.waitWorkflow
import io.infinitic.workflows.engine.handlers.workflowTaskCompleted
import io.infinitic.workflows.engine.handlers.workflowTaskFailed
import io.infinitic.workflows.engine.helpers.commandTerminated
import io.infinitic.workflows.engine.helpers.removeTags
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Instant

class WorkflowEngine(
    val clientName: ClientName,
    storage: WorkflowStateStorage,
    sendToClient: SendToClient,
    sendToTaskTag: SendToTaskTag,
    sendToTaskExecutor: SendToTaskExecutor,
    sendToWorkflowTaskExecutor: SendToTaskExecutor,
    sendToWorkflowTag: SendToWorkflowTag,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToWorkflowEngineAfter: SendToWorkflowEngineAfter
) {
    companion object {
        const val NO_STATE_DISCARDING_REASON = "for having null workflow state"
    }

    private val logger = KotlinLogging.logger {}

    private val storage = LoggedWorkflowStateStorage(storage)

    private val output = WorkflowEngineOutput(
        clientName,
        sendToClient,
        sendToTaskTag,
        sendToTaskExecutor,
        sendToWorkflowTaskExecutor,
        sendToWorkflowTag,
        sendToWorkflowEngine,
        sendToWorkflowEngineAfter
    )

    suspend fun handle(message: WorkflowEngineMessage) {
        logger.debug { "receiving $message" }

        // get current state
        var state = storage.getState(message.workflowId)

        // we can receive a message that has already been processed
        // if a crash happened between the state update and the message acknowledgement
        // in this case, we discard the message
        if (state?.lastMessageId == message.messageId) {
            logDiscardingMessage(message, "as state already contains this messageId")

            return
        }

        state = process(state, message) ?: return // null => discarded message

        when (state.methodRuns.size) {
            // workflow is completed
            0 -> {
                // remove reference to this workflow in tags
                removeTags(output, state)
                // delete state
                storage.delState(message.workflowId)
            }
            // workflow is ongoing
            else -> {
                // record this message as the last processed
                state.lastMessageId = message.messageId
                // update state
                storage.putState(message.workflowId, state)
            }
        }
    }

    private suspend fun process(state: WorkflowState?, message: WorkflowEngineMessage): WorkflowState? =
        coroutineScope {
            // if no state (new or terminated workflow)
            if (state == null) {
                if (message is DispatchWorkflow) {
                    return@coroutineScope dispatchWorkflow(output, message)
                } else if (message is DispatchMethod) {
                    if (message.clientWaiting) {
                        val methodRunUnknown = MethodRunUnknown(
                            recipientName = message.emitterName,
                            message.workflowId,
                            message.methodRunId,
                            emitterName = clientName
                        )
                        launch { output.sendEventsToClient(methodRunUnknown) }
                    }
                    if (message.parentWorkflowId != null && message.parentWorkflowId != message.workflowId) {
                        val childMethodFailed = ChildMethodUnknown(
                            workflowId = message.parentWorkflowId!!,
                            workflowName = message.parentWorkflowName ?: thisShouldNotHappen(),
                            methodRunId = message.parentMethodRunId ?: thisShouldNotHappen(),
                            childUnknownWorkflowError = UnknownWorkflowError(
                                workflowName = message.workflowName,
                                workflowId = message.workflowId,
                                methodRunId = message.methodRunId
                            ),
                            emitterName = clientName
                        )
                        launch { output.sendToWorkflowEngine(childMethodFailed) }
                    }
                } else if (message is WaitWorkflow) {
                    val methodRunUnknown = MethodRunUnknown(
                        recipientName = message.emitterName,
                        message.workflowId,
                        message.methodRunId,
                        emitterName = clientName
                    )
                    launch { output.sendEventsToClient(methodRunUnknown) }
                }

                // discard all other messages if workflow is already terminated
                logDiscardingMessage(message, NO_STATE_DISCARDING_REASON)

                return@coroutineScope null
            }

            // Idempotency: do not relaunch if this workflow has already been launched
            if (message is DispatchWorkflow) {
                logDiscardingMessage(message, "as workflow has already been launched")

                return@coroutineScope null
            }

            // Idempotency: do not relaunch if this method has already been launched
            if (message is DispatchMethod && state.getMethodRun(message.methodRunId) != null) {
                logDiscardingMessage(message, "as this method has already been launched")

                return@coroutineScope null
            }

            // Idempotency: do not relaunch if this signal has already been received
            if (message is SendSignal && state.hasSignalAlreadyBeenReceived(message.signalId)) {
                logDiscardingMessage(message, "as this signal has already been received")

                return@coroutineScope null
            }

            // Idempotency: discard if this workflowTask is not the current one
            if (message.isWorkflowTaskEvent() && (message as TaskEvent).taskId() != state.runningWorkflowTaskId) {
                logDiscardingMessage(message, "as workflowTask is not the right one")

                return@coroutineScope null
            }

            // if a workflow task is ongoing then buffer all WorkflowEvent message,
            // - except for TaskCompleted/TaskFailed/TaskCanceled associated to a WorkflowTask
            if (state.runningWorkflowTaskId != null && message is WorkflowEvent && !message.isWorkflowTaskEvent()) {
                // buffer this message
                logger.debug { "workflowId ${state.workflowId} - buffering $message" }
                state.messagesBuffer.add(message)

                return@coroutineScope state
            }

            // process this message
            processMessage(state, message)

            // process all buffered messages
            while (
                state.runningWorkflowTaskId == null && // if a workflowTask is not ongoing
                state.messagesBuffer.size > 0 // if there is some buffered message
            ) {
                val bufferedMsg = state.messagesBuffer.removeAt(0)
                logger.debug { "workflowId ${bufferedMsg.workflowId} - processing buffered message $bufferedMsg" }
                processMessage(state, bufferedMsg)
            }

            return@coroutineScope state
        }

    private fun logDiscardingMessage(message: WorkflowEngineMessage, cause: String) {
        logger.warn { "workflowId ${message.workflowId} - discarding $cause: $message" }
    }

    private fun CoroutineScope.processMessage(state: WorkflowState, message: WorkflowEngineMessage) {
        // if message is related to a workflowTask, it's not running anymore
        if (message.isWorkflowTaskEvent()) state.runningWorkflowTaskId = null

        // if methodRun has already been cleaned (completed), then discard the message
        if (message is MethodEvent && state.getMethodRun(message.methodRunId) == null) {
            logDiscardingMessage(message, "as null methodRun")

            return
        }

        @Suppress("UNUSED_VARIABLE")
        val m = when (message) {
            is DispatchWorkflow -> thisShouldNotHappen()
            is DispatchMethod -> dispatchMethodRun(output, state, message)
            is CancelWorkflow -> cancelWorkflow(output, state, message)
            is SendSignal -> sendSignal(output, state, message)
            is WaitWorkflow -> waitWorkflow(output, state, message)
            is CompleteTimers -> completeTimer(state, message)
            is CompleteWorkflow -> TODO()
            is RetryWorkflowTask -> retryWorkflowTask(output, state)
            is RetryTasks -> retryTasks(output, state, message)
            is TimerCompleted -> commandTerminated(
                output,
                state,
                message.methodRunId,
                CommandId.from(message.timerId),
                CommandStatus.Completed(
                    returnValue = ReturnValue.from(Instant.now()),
                    completionWorkflowTaskIndex = state.workflowTaskIndex
                )
            )

            is ChildMethodUnknown -> commandTerminated(
                output,
                state,
                message.methodRunId,
                CommandId.from(message.childUnknownWorkflowError.methodRunId ?: thisShouldNotHappen()),
                CommandStatus.Unknown(
                    message.childUnknownWorkflowError,
                    state.workflowTaskIndex
                )
            )

            is ChildMethodCanceled -> commandTerminated(
                output,
                state,
                message.methodRunId,
                CommandId.from(message.childCanceledWorkflowError.methodRunId ?: thisShouldNotHappen()),
                CommandStatus.Canceled(
                    message.childCanceledWorkflowError,
                    state.workflowTaskIndex
                )
            )

            is ChildMethodFailed -> commandTerminated(
                output,
                state,
                message.methodRunId,
                CommandId.from(message.childFailedWorkflowError.methodRunId ?: thisShouldNotHappen()),
                CommandStatus.Failed(
                    message.childFailedWorkflowError,
                    state.workflowTaskIndex
                )
            )

            is ChildMethodCompleted -> commandTerminated(
                output,
                state,
                message.methodRunId,
                CommandId.from(message.childWorkflowReturnValue.methodRunId),
                CommandStatus.Completed(
                    returnValue = message.childWorkflowReturnValue.returnValue,
                    completionWorkflowTaskIndex = state.workflowTaskIndex
                )
            )

            is TaskCanceled -> when (message.isWorkflowTaskEvent()) {
                true -> {
                    TODO()
                }

                false -> commandTerminated(
                    output,
                    state,
                    message.methodRunId,
                    CommandId.from(message.canceledTaskError.taskId),
                    CommandStatus.Canceled(
                        message.canceledTaskError,
                        state.workflowTaskIndex
                    )
                )
            }

            is TaskFailed -> when (message.isWorkflowTaskEvent()) {
                true -> {
                    val msg = workflowTaskFailed(output, state, message)
                    // add fake message at the top of the messagesBuffer list
                    state.messagesBuffer.addAll(0, msg); Unit
                }

                false -> {
                    commandTerminated(
                        output,
                        state,
                        message.methodRunId,
                        CommandId.from(message.failedTaskError.taskId),
                        CommandStatus.Failed(
                            message.failedTaskError,
                            state.workflowTaskIndex
                        )
                    )
                }
            }

            is TaskCompleted -> when (message.isWorkflowTaskEvent()) {
                true -> {
                    val messages = workflowTaskCompleted(output, state, message)
                    // add fake messages at the top of the messagesBuffer list
                    state.messagesBuffer.addAll(0, messages); Unit
                }

                false -> commandTerminated(
                    output,
                    state,
                    message.methodRunId,
                    CommandId.from(message.taskReturnValue.taskId),
                    CommandStatus.Completed(
                        returnValue = message.taskReturnValue.returnValue,
                        completionWorkflowTaskIndex = state.workflowTaskIndex
                    )
                )
            }
        }
    }
}
