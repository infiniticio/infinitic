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

package io.infinitic.tasks.engine

import io.infinitic.common.clients.messages.UnknownTask
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskError
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.plus
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.RetryTaskAttempt
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.engine.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.RemoveTaskTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.tasks.engine.storage.LoggedTaskStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.infinitic.common.clients.messages.TaskCanceled as TaskCanceledInClient
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.workflows.engine.messages.TaskCanceled as TaskCanceledInWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

class TaskEngine(
    storage: TaskStateStorage,
    val sendToClient: SendToClient,
    val sendToTaskTagEngine: SendToTaskTagEngine,
    val sendToTaskEngineAfter: SendToTaskEngineAfter,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToTaskExecutors: SendToTaskExecutors,
    val sendToMetricsPerName: SendToMetricsPerName
) {
    private val storage = LoggedTaskStateStorage(storage)

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskEngineMessage) {
        logger.debug("receiving {}", message)

        // get current state
        val oldState = storage.getState(message.taskId)

        if (oldState == null) {
            // discard message other than DispatchTask if state does not exist
            if (message !is DispatchTask) {
                if (message is WaitTask) {
                    sendToClient(
                        UnknownTask(
                            message.clientName,
                            message.taskId
                        )
                    )
                }
                // is should happen only if a previous retry or a cancel command has terminated this task
                return logDiscardingMessage(message, "for having null state")
            }
        } else {
            if (oldState.lastMessageId == message.messageId) {
                // this message has already been handled
                return logDiscardingMessage(message, "as state already contains this messageId")
            }
            // check is this task has already been launched
            // (For example, DispatchTask can be sent twice if the workflow engine is shutdown when processing a workflowTask)
            if (message is DispatchTask) {
                return logDiscardingMessage(message, "as task has already been launched")
            }
            // discard TaskAttemptMessage other than TaskAttemptCompleted, if state has already evolved
            if (message is TaskAttemptMessage && message !is TaskAttemptCompleted) {
                if ((oldState.taskAttemptId != message.taskAttemptId) ||
                    (oldState.taskRetryIndex != message.taskRetryIndex)
                ) {
                    // is should happen only after a retry command
                    return logDiscardingMessage(message, "as more recent attempt exist")
                }
            }
        }

        val newState =
            if (oldState == null)
                dispatchTask(message as DispatchTask)
            else when (message) {
                is CancelTask -> cancelTask(oldState, message)
                is RetryTask -> retryTask(oldState, message)
                is RetryTaskAttempt -> retryTaskAttempt(oldState, message.messageId)
                is TaskAttemptFailed -> taskAttemptFailed(oldState, message)
                is TaskAttemptCompleted -> taskAttemptCompleted(oldState, message)
                is WaitTask -> waitTask(oldState, message)
                else -> throw RuntimeException("Unknown TaskEngineMessage: $message")
            }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val taskStatusUpdated = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = TaskName("${newState.taskName}::${newState.methodName}"),
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            sendToMetricsPerName(taskStatusUpdated)
        }

        // Update stored state
        if (newState.taskStatus.isTerminated) {
            storage.delState(message.taskId)
        } else if (newState != oldState) {
            storage.putState(message.taskId, newState)
        }
    }

    private fun waitTask(oldState: TaskState, message: WaitTask): TaskState {
        return oldState.copy(
            clientWaiting = oldState.clientWaiting.toMutableSet().plus(message.clientName)
        )
    }

    private suspend fun cancelTask(oldState: TaskState, message: CancelTask): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.TERMINATED_CANCELED
        )

        // if this task belongs to a workflow, send back the TaskCompleted message
        newState.workflowId?.let {
            sendToWorkflowEngine(
                TaskCanceledInWorkflow(
                    workflowId = it,
                    workflowName = newState.workflowName!!,
                    methodRunId = newState.methodRunId!!,
                    taskId = newState.taskId,
                    taskName = newState.taskName
                )
            )
        }

        // if some clients wait for it, send TaskCompleted output back to them
        newState.clientWaiting.map {
            sendToClient(
                TaskCanceledInClient(
                    clientName = it,
                    taskId = newState.taskId,
                    taskMeta = newState.taskMeta
                )
            )
        }

        // delete stored state
        removeTags(newState)

        return newState
    }

    private suspend fun dispatchTask(message: DispatchTask): TaskState {
        // init a state
        val newState = TaskState(
            clientWaiting = when (message.clientWaiting) {
                true -> setOf(message.clientName)
                false -> setOf()
            },
            lastMessageId = message.messageId,
            taskId = message.taskId,
            taskName = message.taskName,
            methodName = message.methodName,
            methodParameterTypes = message.methodParameterTypes,
            methodParameters = message.methodParameters,
            workflowId = message.workflowId,
            workflowName = message.workflowName,
            methodRunId = message.methodRunId,
            taskAttemptId = TaskAttemptId(),
            taskStatus = TaskStatus.RUNNING_OK,
            taskTags = message.taskTags,
            taskOptions = message.taskOptions,
            taskMeta = message.taskMeta
        )

        // send task to workers
        val rt = ExecuteTaskAttempt(
            taskName = newState.taskName,
            taskId = newState.taskId,
            workflowId = newState.workflowId,
            workflowName = newState.workflowName,
            taskAttemptId = newState.taskAttemptId,
            taskRetrySequence = newState.taskRetrySequence,
            taskRetryIndex = newState.taskRetryIndex,
            lastTaskError = null,
            methodName = newState.methodName,
            methodParameterTypes = newState.methodParameterTypes,
            methodParameters = newState.methodParameters,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        sendToTaskExecutors(rt)

        return newState
    }

    private suspend fun retryTask(oldState: TaskState, message: RetryTask): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptId = TaskAttemptId(),
            taskRetryIndex = TaskRetryIndex(0),
            taskRetrySequence = oldState.taskRetrySequence + 1,
            taskName = message.taskName,
            methodName = message.methodName ?: oldState.methodName,
            methodParameters = message.methodParameters ?: oldState.methodParameters,
            methodParameterTypes = message.methodParameterTypes ?: oldState.methodParameterTypes,
            taskOptions = message.taskOptions ?: oldState.taskOptions,
            taskMeta = message.taskMeta ?: oldState.taskMeta
        )

        // send task to workers
        val executeTaskAttempt = ExecuteTaskAttempt(
            taskName = newState.taskName,
            taskId = newState.taskId,
            workflowId = newState.workflowId,
            workflowName = newState.workflowName,
            taskAttemptId = newState.taskAttemptId,
            taskRetrySequence = newState.taskRetrySequence,
            taskRetryIndex = newState.taskRetryIndex,
            lastTaskError = newState.lastTaskError,
            methodName = newState.methodName,
            methodParameterTypes = newState.methodParameterTypes,
            methodParameters = newState.methodParameters,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        sendToTaskExecutors(executeTaskAttempt)

        return newState
    }

    private suspend fun retryTaskAttempt(
        oldState: TaskState,
        messageId: MessageId
    ): TaskState {
        val newState = oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskRetryIndex = oldState.taskRetryIndex + 1
        )

        // send task to workers
        val executeTaskAttempt = ExecuteTaskAttempt(
            taskName = newState.taskName,
            taskId = newState.taskId,
            workflowId = newState.workflowId,
            workflowName = newState.workflowName,
            taskAttemptId = newState.taskAttemptId,
            taskRetrySequence = newState.taskRetrySequence,
            taskRetryIndex = newState.taskRetryIndex,
            lastTaskError = newState.lastTaskError,
            methodName = newState.methodName,
            methodParameterTypes = newState.methodParameterTypes,
            methodParameters = newState.methodParameters,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        sendToTaskExecutors(executeTaskAttempt)

        return newState
    }

    private suspend fun taskAttemptCompleted(oldState: TaskState, message: TaskAttemptCompleted): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.TERMINATED_COMPLETED,
            taskMeta = message.taskMeta
        )

        // if this task belongs to a workflow, send back the adhoc message
        newState.workflowId?.let {
            sendToWorkflowEngine(
                TaskCompletedInWorkflow(
                    workflowId = it,
                    workflowName = newState.workflowName!!,
                    methodRunId = newState.methodRunId!!,
                    taskId = newState.taskId,
                    taskName = newState.taskName,
                    taskReturnValue = message.taskReturnValue
                )
            )
        }

        // if client is waiting, send output back to it
        newState.clientWaiting.map {
            sendToClient(
                TaskCompletedInClient(
                    clientName = it,
                    taskId = newState.taskId,
                    taskReturnValue = message.taskReturnValue,
                    taskMeta = newState.taskMeta
                )
            )
        }

        removeTags(newState)

        return newState
    }

    private suspend fun removeTags(state: TaskState) {
        // remove tags reference to this instance
        state.taskTags.map {
            sendToTaskTagEngine(
                RemoveTaskTag(
                    taskTag = it,
                    taskName = state.taskName,
                    taskId = state.taskId,
                )
            )
        }
    }

    private suspend fun taskAttemptFailed(oldState: TaskState, msg: TaskAttemptFailed): TaskState {
        return delayRetryTaskAttempt(
            oldState = oldState,
            delay = msg.taskAttemptDelayBeforeRetry,
            error = msg.taskAttemptError,
            messageId = msg.messageId,
            taskMeta = msg.taskMeta
        )
    }

    private suspend fun delayRetryTaskAttempt(
        oldState: TaskState,
        delay: MillisDuration?,
        error: TaskError,
        messageId: MessageId,
        taskMeta: TaskMeta
    ): TaskState {
        // no retry
        if (delay == null) return oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_ERROR,
            lastTaskError = error,
            taskMeta = taskMeta
        )
        // immediate retry
        if (delay.long <= 0) return retryTaskAttempt(
            oldState.copy(lastTaskError = error, taskMeta = taskMeta),
            messageId
        )
        // delayed retry
        val newState = oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            lastTaskError = error,
            taskMeta = taskMeta
        )

        // schedule next attempt
        val retryTaskAttempt = RetryTaskAttempt(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskAttemptId = newState.taskAttemptId,
            taskRetrySequence = newState.taskRetrySequence,
            taskRetryIndex = newState.taskRetryIndex
        )
        sendToTaskEngineAfter(retryTaskAttempt, delay)

        return newState
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, reason: String) {
        logger.info("{} - discarding {}", reason, message)
    }
}
