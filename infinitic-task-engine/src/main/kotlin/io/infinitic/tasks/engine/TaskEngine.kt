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

import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.TaskUnknown
import io.infinitic.common.data.ClientName
import io.infinitic.common.errors.CanceledTaskError
import io.infinitic.common.errors.FailedTaskError
import io.infinitic.common.errors.WorkerError
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.plus
import io.infinitic.common.tasks.engines.SendToTaskEngineAfter
import io.infinitic.common.tasks.engines.messages.CancelTask
import io.infinitic.common.tasks.engines.messages.CompleteTask
import io.infinitic.common.tasks.engines.messages.DispatchTask
import io.infinitic.common.tasks.engines.messages.RetryTask
import io.infinitic.common.tasks.engines.messages.RetryTaskAttempt
import io.infinitic.common.tasks.engines.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engines.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engines.messages.TaskEngineMessage
import io.infinitic.common.tasks.engines.messages.WaitTask
import io.infinitic.common.tasks.engines.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.tasks.engines.state.TaskState
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.tasks.engine.storage.LoggedTaskStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import io.infinitic.common.clients.messages.TaskCanceled as TaskCanceledInClient
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedInClient
import io.infinitic.common.workflows.engine.messages.TaskCanceled as TaskCanceledInWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

class TaskEngine(
    val clientName: ClientName,
    storage: TaskStateStorage,
    val sendToClient: SendToClient,
    val sendToTaskTag: SendToTaskTag,
    val sendToTaskEngineAfter: SendToTaskEngineAfter,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToTaskExecutors: SendToTaskExecutor
) {
    private val storage = LoggedTaskStateStorage(storage)

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: TaskEngineMessage) {
        val state = process(message) ?: return

        when (state.taskStatus.isTerminated) {
            false -> storage.putState(message.taskId, state)
            true -> storage.delState(message.taskId)
        }
    }

    private suspend fun process(message: TaskEngineMessage): TaskState? = coroutineScope {
        logger.debug { "receiving $message" }

        // get current state
        val state = storage.getState(message.taskId)

        if (state == null) {
            if (message is DispatchTask) {

                return@coroutineScope dispatchTask(message)
            }

            if (message is WaitTask) {
                val taskUnknown = TaskUnknown(
                    recipientName = message.emitterName,
                    taskId = message.taskId,
                    emitterName = clientName
                )
                launch { sendToClient(taskUnknown) }
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
            is DispatchTask -> thisShouldNotHappen()
        }

        return@coroutineScope state
    }

    private fun CoroutineScope.waitTask(state: TaskState, message: WaitTask) {
        when (state.taskStatus) {
            TaskStatus.TERMINATED_COMPLETED -> {
                val taskCompleted = TaskCompletedInClient(
                    emitterName = clientName,
                    recipientName = message.emitterName,
                    taskId = state.taskId,
                    taskReturnValue = state.taskReturnValue!!,
                    taskMeta = state.taskMeta
                )
                launch { sendToClient(taskCompleted) }
            }
            TaskStatus.TERMINATED_CANCELED -> {
                val taskCanceled = TaskCanceledInClient(
                    emitterName = clientName,
                    recipientName = message.emitterName,
                    taskId = state.taskId,
                )
                launch { sendToClient(taskCanceled) }
            }
            TaskStatus.RUNNING_ERROR -> {
                val lastError = state.lastError ?: thisShouldNotHappen()
                val taskFailed = TaskFailedInClient(
                    emitterName = clientName,
                    recipientName = message.emitterName,
                    taskId = state.taskId,
                    error = lastError,
                )
                launch { sendToClient(taskFailed) }
            }
            TaskStatus.RUNNING_OK, TaskStatus.RUNNING_WARNING ->
                state.waitingClients.add(message.emitterName)
        }
    }

    private fun CoroutineScope.dispatchTask(message: DispatchTask): TaskState {
        // init a state
        val state = TaskState(
            waitingClients = when (message.clientWaiting) {
                true -> mutableSetOf(message.emitterName)
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
            taskOptions = message.taskOptions,
            taskTags = message.taskTags,
            taskMeta = message.taskMeta
        )

        // send task to workers
        val executeTaskAttempt = ExecuteTaskAttempt(
            taskName = state.taskName,
            taskId = state.taskId,
            taskTags = state.taskTags,
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            taskAttemptId = state.taskAttemptId,
            taskRetrySequence = state.taskRetrySequence,
            taskRetryIndex = state.taskRetryIndex,
            lastError = null,
            methodName = state.methodName,
            methodParameterTypes = state.methodParameterTypes,
            methodParameters = state.methodParameters,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta,
            emitterName = clientName
        )
        launch { sendToTaskExecutors(executeTaskAttempt) }

        return state
    }

    private fun CoroutineScope.cancelTask(state: TaskState) {
        state.taskStatus = TaskStatus.TERMINATED_CANCELED

        // if this task belongs to a workflow, send back the TaskCompleted message
        state.workflowId?.let {
            val taskCanceled = TaskCanceledInWorkflow(
                emitterName = clientName,
                workflowId = it,
                workflowName = state.workflowName!!,
                methodRunId = state.methodRunId!!,
                canceledTaskError = CanceledTaskError(
                    taskName = state.taskName,
                    taskId = state.taskId,
                    methodName = state.methodName,
                )
            )
            launch { sendToWorkflowEngine(taskCanceled) }
        }

        // if some clients wait for it, send TaskCompleted output back to them
        state.waitingClients.map {
            val taskCanceledInClient = TaskCanceledInClient(
                emitterName = clientName,
                recipientName = it,
                taskId = state.taskId
            )
            launch { sendToClient(taskCanceledInClient) }
        }
        state.waitingClients.clear()

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
            taskTags = state.taskTags,
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
            taskMeta = state.taskMeta,
            emitterName = clientName
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
            taskTags = state.taskTags,
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
            taskMeta = state.taskMeta,
            emitterName = clientName
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
                emitterName = clientName,
                workflowId = it,
                workflowName = state.workflowName!!,
                methodRunId = state.methodRunId!!,
                taskReturnValue = TaskReturnValue(
                    taskId = state.taskId,
                    taskName = state.taskName,
                    returnValue = message.taskReturnValue
                )
            )
            launch { sendToWorkflowEngine(taskCompleted) }
        }

        // if client is waiting, send output back to it
        state.waitingClients.forEach {
            val taskCompleted = TaskCompletedInClient(
                emitterName = clientName,
                recipientName = it,
                taskId = state.taskId,
                taskReturnValue = message.taskReturnValue,
                taskMeta = state.taskMeta
            )
            launch { sendToClient(taskCompleted) }
        }
        state.waitingClients.clear()

        removeTags(state)
    }

    private fun CoroutineScope.removeTags(state: TaskState) {
        // remove tags reference to this instance
        state.taskTags.map {
            val removeTagFromTask = RemoveTagFromTask(
                taskName = state.taskName,
                taskTag = it,
                taskId = state.taskId,
                emitterName = clientName,
            )
            launch { sendToTaskTag(removeTagFromTask) }
        }
    }

    private fun CoroutineScope.taskAttemptFailed(state: TaskState, msg: TaskAttemptFailed) {
        with(state) {
            lastError = msg.workerError
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
                        workflowName = state.workflowName!!,
                        workflowId = it,
                        methodRunId = state.methodRunId!!,
                        emitterName = clientName,
                        failedTaskError = FailedTaskError(
                            taskName = msg.taskName,
                            taskId = msg.taskId,
                            methodName = state.methodName,
                            cause = msg.workerError ?: WorkerError.from(ClientName("unsused"), Exception("unused"))
                        ),
                        deferredError = msg.deferredError
                    )
                    launch { sendToWorkflowEngine(taskFailed) }
                }
                // tell waiting clients
                state.waitingClients.forEach {
                    val taskFailed = TaskFailedInClient(
                        emitterName = clientName,
                        recipientName = it,
                        taskId = state.taskId,
                        error = msg.workerError ?: thisShouldNotHappen(),
                    )
                    launch { sendToClient(taskFailed) }
                }
                state.waitingClients.clear()
            }
            // immediate retry
            delay.long <= 0 -> retryTaskAttempt(state)
            // delayed retry
            else -> {
                state.taskStatus = TaskStatus.RUNNING_WARNING

                // schedule next attempt
                val retryTaskAttempt = RetryTaskAttempt(
                    taskName = state.taskName,
                    taskId = state.taskId,
                    taskRetryIndex = state.taskRetryIndex,
                    taskAttemptId = state.taskAttemptId,
                    taskRetrySequence = state.taskRetrySequence,
                    emitterName = clientName
                )
                launch { sendToTaskEngineAfter(retryTaskAttempt, delay) }
            }
        }
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, cause: String) {
        logger.warn { "$cause - discarding $message" }
    }
}
