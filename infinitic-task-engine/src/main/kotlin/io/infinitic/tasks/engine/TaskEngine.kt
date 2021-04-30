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
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.data.TaskAttemptId
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
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.tasks.engine.storage.LoggedTaskStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext
import io.infinitic.common.clients.messages.TaskCanceled as TaskCanceledInClient
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedInClient
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

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskEngineMessage) {
        logger.warn("receiving {}", message)

        // get current state
        val oldState = storage.getState(message.taskId)

        if (oldState == null) {
            // discard message other than DispatchTask if state does not exist
            if (message !is DispatchTask) {
                if (message is WaitTask) {
                    val unknownTask = UnknownTask(message.clientName, message.taskId)
                    sendToClient(unknownTask)
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
            // discard all message (except client request is already terminated)
            if (oldState.taskStatus.isTerminated && message !is WaitTask) {
                return logDiscardingMessage(message, "as task is already terminated")
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
        if (newState != oldState) {
            storage.putState(message.taskId, newState)
        }

        // delete state if terminated
        // the delay makes tests easier, avoiding failure of synchronous requests
        if (newState.taskStatus.isTerminated) {
            CoroutineScope(coroutineContext).launch {
                delay(200L)
                storage.delState(message.taskId)
            }
        }
    }

    private suspend fun waitTask(oldState: TaskState, message: WaitTask): TaskState =
        when (oldState.taskStatus) {
            TaskStatus.TERMINATED_COMPLETED -> {
                val taskCompleted = TaskCompletedInClient(
                    clientName = message.clientName,
                    taskId = oldState.taskId,
                    taskReturnValue = oldState.taskReturnValue!!,
                    taskMeta = oldState.taskMeta
                )
                sendToClient(taskCompleted)

                oldState
            }
            TaskStatus.TERMINATED_CANCELED -> {
                val taskCanceled = TaskCanceledInClient(
                    clientName = message.clientName,
                    taskId = oldState.taskId,
                    taskMeta = oldState.taskMeta
                )
                sendToClient(taskCanceled)

                oldState
            }
            else -> oldState.copy(
                waitingClients = oldState.waitingClients.toMutableSet().plus(message.clientName)
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
        newState.waitingClients.map {
            val taskCanceledInClient = TaskCanceledInClient(
                clientName = it,
                taskId = newState.taskId,
                taskMeta = newState.taskMeta
            )
            sendToClient(taskCanceledInClient)
        }

        // delete stored state
        removeTags(newState)

        return newState
    }

    private suspend fun dispatchTask(message: DispatchTask): TaskState {
        // init a state
        val newState = TaskState(
            waitingClients = when (message.clientWaiting) {
                true -> setOf(message.clientName)
                false -> setOf()
            },
            lastMessageId = message.messageId,
            taskId = message.taskId,
            taskName = message.taskName,
            taskReturnValue = null,
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
            lastError = null,
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
            taskStatus = TaskStatus.RUNNING_OK,
            taskAttemptId = TaskAttemptId(),
            taskRetryIndex = TaskRetryIndex(0),
            taskRetrySequence = oldState.taskRetrySequence + 1
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
            lastError = newState.lastError,
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
            lastError = newState.lastError,
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
            taskReturnValue = message.taskReturnValue,
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.TERMINATED_COMPLETED,
            taskMeta = message.taskMeta
        )

        // if this task belongs to a workflow, send back the adhoc message
        newState.workflowId?.let {
            val taskCompleted = TaskCompletedInWorkflow(
                workflowId = it,
                workflowName = newState.workflowName!!,
                methodRunId = newState.methodRunId!!,
                taskId = newState.taskId,
                taskName = newState.taskName,
                taskReturnValue = message.taskReturnValue
            )
            sendToWorkflowEngine(taskCompleted)
        }

        // if client is waiting, send output back to it
        newState.waitingClients.forEach {
            val taskCompleted = TaskCompletedInClient(
                clientName = it,
                taskId = newState.taskId,
                taskReturnValue = message.taskReturnValue,
                taskMeta = newState.taskMeta
            )
            sendToClient(taskCompleted)
        }

        removeTags(newState)

        return newState
    }

    private suspend fun removeTags(state: TaskState) {
        // remove tags reference to this instance
        state.taskTags.map {
            val removeTaskTag = RemoveTaskTag(
                taskTag = it,
                taskName = state.taskName,
                taskId = state.taskId,
            )
            sendToTaskTagEngine(removeTaskTag)
        }
    }

    private suspend fun taskAttemptFailed(oldState: TaskState, msg: TaskAttemptFailed): TaskState {

        val delay = msg.taskAttemptDelayBeforeRetry
        val error = msg.taskAttemptError
        val messageId = msg.messageId
        val taskMeta = msg.taskMeta

        return when {
            // no retry => task failed
            delay == null -> {
                // tell parent workflow
                oldState.workflowId?.let {
                    val taskFailed = TaskFailed(
                        workflowId = it,
                        workflowName = oldState.workflowName!!,
                        methodRunId = oldState.methodRunId!!,
                        taskId = oldState.taskId,
                        taskName = oldState.taskName,
                        error = error
                    )
                    sendToWorkflowEngine(taskFailed)
                }
                // tell waiting clients
                oldState.waitingClients.forEach {
                    val taskFailed = TaskFailedInClient(
                        clientName = it,
                        taskId = oldState.taskId,
                        error = error,
                    )
                    sendToClient(taskFailed)
                }

                oldState.copy(
                    lastMessageId = messageId,
                    taskStatus = TaskStatus.RUNNING_ERROR,
                    lastError = error,
                    taskMeta = taskMeta,
                    waitingClients = setOf()
                )
            }
            // immediate retry
            delay.long <= 0 -> retryTaskAttempt(
                oldState.copy(lastError = error, taskMeta = taskMeta),
                messageId
            )
            // delayed retry
            else -> {
                // schedule next attempt
                val retryTaskAttempt = RetryTaskAttempt(
                    taskId = oldState.taskId,
                    taskName = oldState.taskName,
                    taskAttemptId = oldState.taskAttemptId,
                    taskRetrySequence = oldState.taskRetrySequence,
                    taskRetryIndex = oldState.taskRetryIndex
                )
                sendToTaskEngineAfter(retryTaskAttempt, delay)

                oldState.copy(
                    lastMessageId = messageId,
                    taskStatus = TaskStatus.RUNNING_WARNING,
                    lastError = error,
                    taskMeta = taskMeta
                )
            }
        }
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, reason: String) {
        logger.info("{} - discarding {}", reason, message)
    }
}
