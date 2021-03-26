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

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.RemoveTaskTag
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.plus
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.RetryTaskAttempt
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskAttemptStarted
import io.infinitic.common.tasks.engine.messages.TaskCanceled
import io.infinitic.common.tasks.engine.messages.TaskCompleted
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.engine.messages.WorkflowTaskCompleted
import io.infinitic.tasks.engine.storage.events.TaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateStorage
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedInClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

class TaskEngine(
    private val taskStateStorage: TaskStateStorage,
    private val taskEventStorage: TaskEventStorage,
    private val taskEngineOutput: TaskEngineOutput
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskEngineMessage) {
        logger.debug("taskId {} - receiving {}", message.taskId, message)

        // store event
        taskEventStorage.insertTaskEvent(message)

        // immediately discard messages useless messages
        when (message) {
            is TaskAttemptDispatched -> return
            is TaskAttemptStarted -> return
            is TaskCompleted -> return
            is TaskCanceled -> return
            else -> Unit
        }

        // get current state
        val oldState = taskStateStorage.getState(message.taskId)

        if (oldState == null) {
            // discard message other than DispatchTask if state does not exist
            if (message !is DispatchTask) {
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
                    (oldState.taskAttemptRetry != message.taskAttemptRetry)
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
                is TaskAttemptStarted -> taskAttemptStarted(oldState, message)
                is TaskAttemptFailed -> taskAttemptFailed(oldState, message)
                is TaskAttemptCompleted -> taskAttemptCompleted(oldState, message)
                else -> throw Exception("Unknown TaskEngineMessage: $message")
            }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.taskStatus.isTerminated) {
            taskStateStorage.putState(message.taskId, newState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val tsc = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = TaskName("${newState.taskName}::${newState.methodName}"),
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            taskEngineOutput.sendToMonitoringPerName(newState, tsc)
        }
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, reason: String) {
        logger.info("taskId {} - discarding {}: {} (messageId {})", message.taskId, reason, message, message.messageId)
    }

    private suspend fun cancelTask(oldState: TaskState, message: CancelTask): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.TERMINATED_CANCELED
        )

        // if this task belongs to a workflow, send back the TaskCompleted message
        newState.workflowId?.let {
            taskEngineOutput.sendToWorkflowEngine(
                newState,
                when ("${newState.taskName}") {
                    WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                        workflowId = it,
                        workflowTaskId = WorkflowTaskId(newState.taskId.id),
                        workflowTaskReturnValue = message.taskReturnValue.get() as WorkflowTaskReturnValue
                    )
                    else -> TaskCompletedInWorkflow(
                        workflowId = it,
                        methodRunId = newState.methodRunId!!,
                        taskId = newState.taskId,
                        taskReturnValue = message.taskReturnValue
                    )
                },
                MillisDuration(0)
            )
        }

        // if this task comes from a client, send TaskCompleted output back to it
        if (newState.clientWaiting) {
            taskEngineOutput.sendToClientResponse(
                newState,
                TaskCompletedInClient(
                    newState.clientName,
                    newState.taskId,
                    message.taskReturnValue
                )
            )
        }

        // log event
        val tad = TaskCanceled(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskReturnValue = message.taskReturnValue,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskEngine(newState, tad, MillisDuration(0))

        // Delete stored state
        taskStateStorage.delState(newState.taskId)

        return newState
    }

    private suspend fun dispatchTask(message: DispatchTask): TaskState {
        // init a state
        val newState = TaskState(
            clientName = message.clientName,
            clientWaiting = message.clientWaiting,
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
            taskOptions = message.taskOptions,
            taskMeta = message.taskMeta
        )

        // send task to workers
        val rt = ExecuteTaskAttempt(
            taskId = newState.taskId,
            taskRetry = newState.taskRetry,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskName = newState.taskName,
            methodName = newState.methodName,
            methodParameters = newState.methodParameters,
            methodParameterTypes = newState.methodParameterTypes,
            previousTaskAttemptError = null,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(newState, rt)

        // log events
        val tad = TaskAttemptDispatched(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry
        )
        taskEngineOutput.sendToTaskEngine(newState, tad, MillisDuration(0))

        return newState
    }

    private suspend fun retryTask(oldState: TaskState, message: RetryTask): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptId = TaskAttemptId(),
            taskAttemptRetry = TaskAttemptRetry(0),
            taskRetry = oldState.taskRetry + 1,
            taskName = message.taskName,
            methodName = message.methodName ?: oldState.methodName,
            methodParameters = message.methodParameters ?: oldState.methodParameters,
            methodParameterTypes = message.methodParameterTypes ?: oldState.methodParameterTypes,
            taskOptions = message.taskOptions ?: oldState.taskOptions,
            taskMeta = message.taskMeta ?: oldState.taskMeta
        )

        // send task to workers
        val rt = ExecuteTaskAttempt(
            taskId = newState.taskId,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry,
            taskName = newState.taskName,
            methodName = newState.methodName,
            methodParameters = newState.methodParameters,
            methodParameterTypes = newState.methodParameterTypes,
            previousTaskAttemptError = newState.previousTaskAttemptError,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(newState, rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry
        )
        taskEngineOutput.sendToTaskEngine(newState, tad, MillisDuration(0))

        return newState
    }

    private suspend fun retryTaskAttempt(oldState: TaskState, messageId: MessageId): TaskState {
        val state = oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptRetry = oldState.taskAttemptRetry + 1
        )

        // send task to workers
        val rt = ExecuteTaskAttempt(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskRetry = state.taskRetry,
            taskName = state.taskName,
            methodName = state.methodName,
            methodParameterTypes = state.methodParameterTypes,
            methodParameters = state.methodParameters,
            previousTaskAttemptError = state.previousTaskAttemptError,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(state, rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskName = state.taskName,
            taskAttemptId = state.taskAttemptId,
            taskRetry = state.taskRetry,
            taskAttemptRetry = state.taskAttemptRetry
        )
        taskEngineOutput.sendToTaskEngine(state, tar, MillisDuration(0))

        return state
    }

    private fun taskAttemptStarted(oldState: TaskState, message: TaskAttemptStarted): TaskState {
        return oldState.copy(
            lastMessageId = message.messageId
        )
    }

    private suspend fun taskAttemptCompleted(oldState: TaskState, message: TaskAttemptCompleted): TaskState {
        val newState = oldState.copy(
            lastMessageId = message.messageId,
            taskStatus = TaskStatus.TERMINATED_COMPLETED
        )

        // if this task belongs to a workflow, send back the adhoc message
        newState.workflowId?.let {
            taskEngineOutput.sendToWorkflowEngine(
                newState,
                when ("${newState.taskName}") {
                    WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                        workflowId = it,
                        workflowTaskId = WorkflowTaskId(newState.taskId.id),
                        workflowTaskReturnValue = message.taskReturnValue.get() as WorkflowTaskReturnValue
                    )
                    else -> TaskCompletedInWorkflow(
                        workflowId = it,
                        methodRunId = newState.methodRunId!!,
                        taskId = newState.taskId,
                        taskReturnValue = message.taskReturnValue
                    )
                },
                MillisDuration(0)
            )
        }

        // if client is waiting, send output back to it
        if (newState.clientWaiting) {
            taskEngineOutput.sendToClientResponse(
                newState,
                TaskCompletedInClient(
                    newState.clientName,
                    newState.taskId,
                    message.taskReturnValue
                )
            )
        }

        // log event
        val tc = TaskCompleted(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskReturnValue = message.taskReturnValue,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskEngine(newState, tc, MillisDuration(0))

        // remove tags reference to this instance
        val tags = newState.taskOptions.tags.map { Tag(it) } + Tag.of(newState.taskId)
        tags.map {
            taskEngineOutput.sendToTagEngine(
                newState,
                RemoveTaskTag(
                    tag = it,
                    name = newState.taskName,
                    taskId = newState.taskId,
                )
            )
        }

        // delete stored state
        taskStateStorage.delState(newState.taskId)

        return newState
    }

    private suspend fun taskAttemptFailed(oldState: TaskState, msg: TaskAttemptFailed): TaskState {
        return delayRetryTaskAttempt(
            oldState,
            delay = msg.taskAttemptDelayBeforeRetry,
            error = msg.taskAttemptError,
            msg.messageId
        )
    }

    private suspend fun delayRetryTaskAttempt(
        oldState: TaskState,
        delay: MillisDuration?,
        error: TaskAttemptError,
        messageId: MessageId
    ): TaskState {
        // no retry
        if (delay == null) return oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_ERROR,
            previousTaskAttemptError = error
        )
        // immediate retry
        if (delay.long <= 0) return retryTaskAttempt(oldState.copy(previousTaskAttemptError = error), messageId)
        // delayed retry
        val newState = oldState.copy(
            lastMessageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            previousTaskAttemptError = error
        )

        // schedule next attempt
        val tar = RetryTaskAttempt(
            taskId = newState.taskId,
            taskName = newState.taskName,
            taskRetry = newState.taskRetry,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry
        )
        taskEngineOutput.sendToTaskEngine(newState, tar, delay)

        return newState
    }
}
