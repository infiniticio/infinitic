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
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.plus
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.CompleteTask
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
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.tasks.engine.storage.LoggedTaskStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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
        val state = process(message) ?: return

        // Update stored state
        storage.putState(message.taskId, state)

        // delete state if terminated
        // the delay makes tests easier, avoiding failure of synchronous requests
        if (state.taskStatus.isTerminated) {
            CoroutineScope(coroutineContext).launch {
                delay(200L)
                storage.delState(message.taskId)
            }
        }
    }

    private suspend fun process(message: TaskEngineMessage): TaskState? = coroutineScope {
        logger.warn("receiving {}", message)

        // get current state
        val state = storage.getState(message.taskId)

        if (state == null) {
            if (message is DispatchTask) {
                val newState = dispatchTask(message)
                taskStatusUpdate(newState, null)

                return@coroutineScope newState
            }

            if (message is WaitTask) {
                val unknownTask = UnknownTask(message.clientName, message.taskId)
                launch { sendToClient(unknownTask) }
            }

            // is should happen only if a previous retry or a cancel command has terminated this task
            logDiscardingMessage(message, "for having null state")

            return@coroutineScope null
        }

        if (state.lastMessageId == message.messageId) {
            // this message has already been handled
            logDiscardingMessage(message, "as state already contains this messageId")

            return@coroutineScope null
        }

        // check is this task has already been launched
        // (For example, DispatchTask can be sent twice if the workflow engine is shutdown when processing a workflowTask)
        if (message is DispatchTask) {
            logDiscardingMessage(message, "as task has already been launched")

            return@coroutineScope null
        }

        // discard TaskAttemptMessage other than TaskAttemptCompleted, if state has already evolved
        if (message is TaskAttemptMessage &&
            message !is TaskAttemptCompleted &&
            ((state.taskAttemptId != message.taskAttemptId) || (state.taskRetryIndex != message.taskRetryIndex))
        ) {
            // is should happen only after a retry command
            logDiscardingMessage(message, "as more recent attempt exist")

            return@coroutineScope null
        }

        // discard all message (except client request is already terminated)
        if (state.taskStatus.isTerminated && message !is WaitTask) {
            logDiscardingMessage(message, "as task is already terminated")

            return@coroutineScope null
        }

        state.lastMessageId = message.messageId
        val oldStatus = state.taskStatus

        when (message) {
            is CancelTask -> cancelTask(state)
            is RetryTask -> retryTask(state)
            is RetryTaskAttempt -> retryTaskAttempt(state)
            is TaskAttemptFailed -> taskAttemptFailed(state, message)
            is TaskAttemptCompleted -> taskAttemptCompleted(state, message)
            is WaitTask -> waitTask(state, message)
            is CompleteTask -> TODO()
            is DispatchTask -> throw thisShouldNotHappen()
        }

        // Send TaskStatusUpdated if needed
        if (state.taskStatus != oldStatus) taskStatusUpdate(state, oldStatus)

        return@coroutineScope state
    }

    private fun CoroutineScope.taskStatusUpdate(state: TaskState, oldStatus: TaskStatus?) {
        val taskStatusUpdated = TaskStatusUpdated(
            taskId = state.taskId,
            taskName = TaskName("${state.taskName}::${state.methodName}"),
            oldStatus = oldStatus,
            newStatus = state.taskStatus
        )

        launch { sendToMetricsPerName(taskStatusUpdated) }
    }

    private fun CoroutineScope.waitTask(state: TaskState, message: WaitTask) {
        when (state.taskStatus) {
            TaskStatus.TERMINATED_COMPLETED -> {
                val taskCompleted = TaskCompletedInClient(
                    clientName = message.clientName,
                    taskId = state.taskId,
                    taskReturnValue = state.taskReturnValue!!,
                    taskMeta = state.taskMeta
                )
                launch { sendToClient(taskCompleted) }
            }
            TaskStatus.TERMINATED_CANCELED -> {
                val taskCanceled = TaskCanceledInClient(
                    clientName = message.clientName,
                    taskId = state.taskId,
                    taskMeta = state.taskMeta
                )
                launch { sendToClient(taskCanceled) }
            }
            else -> state.waitingClients.add(message.clientName)
        }
    }

    private fun CoroutineScope.dispatchTask(message: DispatchTask): TaskState {
        // init a state
        val newState = TaskState(
            waitingClients = when (message.clientWaiting) {
                true -> mutableSetOf(message.clientName)
                false -> mutableSetOf()
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
        val executeTaskAttempt = ExecuteTaskAttempt(
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
        launch { sendToTaskExecutors(executeTaskAttempt) }

        return newState
    }

    private fun CoroutineScope.cancelTask(state: TaskState) {
        state.taskStatus = TaskStatus.TERMINATED_CANCELED

        // if this task belongs to a workflow, send back the TaskCompleted message
        state.workflowId?.let {
            val taskCanceled = TaskCanceledInWorkflow(
                workflowId = it,
                workflowName = state.workflowName!!,
                methodRunId = state.methodRunId!!,
                taskId = state.taskId,
                taskName = state.taskName
            )
            launch { sendToWorkflowEngine(taskCanceled) }
        }

        // if some clients wait for it, send TaskCompleted output back to them
        state.waitingClients.map {
            val taskCanceledInClient = TaskCanceledInClient(
                clientName = it,
                taskId = state.taskId,
                taskMeta = state.taskMeta
            )
            launch { sendToClient(taskCanceledInClient) }
        }

        // delete stored state
        removeTags(state)
    }

    private fun CoroutineScope.retryTask(state: TaskState) {
        with(state) {
            taskStatus = TaskStatus.RUNNING_OK
            taskAttemptId = TaskAttemptId()
            taskRetryIndex = TaskRetryIndex(0)
            taskRetrySequence = state.taskRetrySequence + 1
        }

        // send task to workers
        val executeTaskAttempt = ExecuteTaskAttempt(
            taskName = state.taskName,
            taskId = state.taskId,
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            taskAttemptId = state.taskAttemptId,
            taskRetrySequence = state.taskRetrySequence,
            taskRetryIndex = state.taskRetryIndex,
            lastError = state.lastError,
            methodName = state.methodName,
            methodParameterTypes = state.methodParameterTypes,
            methodParameters = state.methodParameters,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        launch { sendToTaskExecutors(executeTaskAttempt) }
    }

    private fun CoroutineScope.retryTaskAttempt(state: TaskState) {
        with(state) {
            taskStatus = TaskStatus.RUNNING_WARNING
            taskRetryIndex = state.taskRetryIndex + 1
        }

        // send task to workers
        val executeTaskAttempt = ExecuteTaskAttempt(
            taskName = state.taskName,
            taskId = state.taskId,
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            taskAttemptId = state.taskAttemptId,
            taskRetrySequence = state.taskRetrySequence,
            taskRetryIndex = state.taskRetryIndex,
            lastError = state.lastError,
            methodName = state.methodName,
            methodParameterTypes = state.methodParameterTypes,
            methodParameters = state.methodParameters,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        launch { sendToTaskExecutors(executeTaskAttempt) }
    }

    private fun CoroutineScope.taskAttemptCompleted(state: TaskState, message: TaskAttemptCompleted) {
        with(state) {
            taskReturnValue = message.taskReturnValue
            taskStatus = TaskStatus.TERMINATED_COMPLETED
            taskMeta = message.taskMeta
        }

        // if this task belongs to a workflow, send back the adhoc message
        state.workflowId?.let {
            val taskCompleted = TaskCompletedInWorkflow(
                workflowId = it,
                workflowName = state.workflowName!!,
                methodRunId = state.methodRunId!!,
                taskId = state.taskId,
                taskName = state.taskName,
                taskReturnValue = message.taskReturnValue
            )
            launch { sendToWorkflowEngine(taskCompleted) }
        }

        // if client is waiting, send output back to it
        state.waitingClients.forEach {
            val taskCompleted = TaskCompletedInClient(
                clientName = it,
                taskId = state.taskId,
                taskReturnValue = message.taskReturnValue,
                taskMeta = state.taskMeta
            )
            launch {
                sendToClient(taskCompleted)
                state.waitingClients.remove(it)
            }
        }

        removeTags(state)
    }

    private fun CoroutineScope.removeTags(state: TaskState) {
        // remove tags reference to this instance
        state.taskTags.map {
            val removeTaskTag = RemoveTaskTag(
                taskTag = it,
                taskName = state.taskName,
                taskId = state.taskId,
            )
            launch { sendToTaskTagEngine(removeTaskTag) }
        }
    }

    private fun CoroutineScope.taskAttemptFailed(state: TaskState, msg: TaskAttemptFailed) {
        with(state) {
            lastError = msg.taskAttemptError
            taskMeta = msg.taskMeta
        }

        val delay = msg.taskAttemptDelayBeforeRetry

        when {
            // no retry => task failed
            delay == null -> {
                state.taskStatus = TaskStatus.RUNNING_ERROR

                // tell parent workflow
                state.workflowId?.let {
                    val taskFailed = TaskFailed(
                        workflowId = it,
                        workflowName = state.workflowName!!,
                        methodRunId = state.methodRunId!!,
                        taskId = state.taskId,
                        taskName = state.taskName,
                        error = msg.taskAttemptError
                    )
                    launch { sendToWorkflowEngine(taskFailed) }
                }
                // tell waiting clients
                state.waitingClients.forEach {
                    val taskFailed = TaskFailedInClient(
                        clientName = it,
                        taskId = state.taskId,
                        error = msg.taskAttemptError,
                    )
                    launch {
                        sendToClient(taskFailed)
                        state.waitingClients.remove(it)
                    }
                }
            }
            // immediate retry
            delay.long <= 0 -> retryTaskAttempt(state)
            // delayed retry
            else -> {
                state.taskStatus = TaskStatus.RUNNING_WARNING

                // schedule next attempt
                val retryTaskAttempt = RetryTaskAttempt(
                    taskId = state.taskId,
                    taskName = state.taskName,
                    taskAttemptId = state.taskAttemptId,
                    taskRetrySequence = state.taskRetrySequence,
                    taskRetryIndex = state.taskRetryIndex
                )
                launch { sendToTaskEngineAfter(retryTaskAttempt, delay) }
            }
        }
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, reason: String) {
        logger.info("{} - discarding {}", reason, message)
    }
}
