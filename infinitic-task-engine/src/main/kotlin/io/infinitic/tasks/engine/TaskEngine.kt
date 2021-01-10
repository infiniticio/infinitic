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

import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
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
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.engine.messages.WorkflowTaskCompleted
import io.infinitic.tasks.engine.storage.events.TaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateStorage
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedInWorkflow

class TaskEngine(
    private val taskStateStorage: TaskStateStorage,
    private val taskEventStorage: TaskEventStorage,
    private val taskEngineOutput: TaskEngineOutput
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskEngineMessage, messageId: String?) {
        logger.debug("taskId {} - messageId {} - receiving {}", message.taskId, messageId, message)

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
                return logDiscardingMessage(message, messageId, "for having null state")
            }
        } else {
            if (messageId != null && oldState.messageId == messageId) {
                // this message has already been handled
                return logDiscardingMessage(message, messageId, "as state already contains this messageId")
            }
            // discard TaskAttemptMessage other than TaskAttemptCompleted, if state has already evolved
            if (message is TaskAttemptMessage && message !is TaskAttemptCompleted) {
                if ((oldState.taskAttemptId != message.taskAttemptId) ||
                    (oldState.taskAttemptRetry != message.taskAttemptRetry)
                ) {
                    // is should happen only after a retry command
                    return logDiscardingMessage(message, messageId, "as more recent attempt exist")
                }
            }
        }

        val newState =
            if (oldState == null)
                dispatchTask(message as DispatchTask, messageId)
            else when (message) {
                is CancelTask -> cancelTask(oldState, message, messageId)
                is RetryTask -> retryTask(oldState, message, messageId)
                is RetryTaskAttempt -> retryTaskAttempt(oldState, messageId)
                is TaskAttemptStarted -> taskAttemptStarted(oldState, message, messageId)
                is TaskAttemptFailed -> taskAttemptFailed(oldState, message, messageId)
                is TaskAttemptCompleted -> taskAttemptCompleted(oldState, message, messageId)
                else -> throw Exception("Unknown EngineMessage: $message")
            }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.taskStatus.isTerminated) {
            taskStateStorage.updateState(message.taskId, newState, oldState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val tsc = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = TaskName("${newState.taskName}::${newState.methodName}"),
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            taskEngineOutput.sendToMonitoringPerName(message.taskId, tsc)
        }
    }

    private fun logDiscardingMessage(message: TaskEngineMessage, messageId: String?, reason: String) {
        logger.info("taskId {} - messageId {} - discarding {}: {}", message.taskId, messageId, reason, message)
    }

    private suspend fun cancelTask(oldState: TaskState, msg: CancelTask, messageId: String?): TaskState {
        val newState = oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.TERMINATED_CANCELED
        )

        // log event
        val tad = TaskCanceled(
            taskId = newState.taskId,
            taskOutput = msg.taskOutput,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskEngine(newState.taskId, tad, 0F)

        // Delete stored state
        taskStateStorage.deleteState(newState.taskId)

        return newState
    }

    private suspend fun dispatchTask(msg: DispatchTask, messageId: String?): TaskState {
        // init a state
        val newState = TaskState(
            messageId = messageId,
            taskId = msg.taskId,
            taskName = msg.taskName,
            methodName = msg.methodName,
            methodParameterTypes = msg.methodParameterTypes,
            methodInput = msg.methodInput,
            workflowId = msg.workflowId,
            methodRunId = msg.methodRunId,
            taskAttemptId = TaskAttemptId(),
            taskStatus = TaskStatus.RUNNING_OK,
            taskOptions = msg.taskOptions,
            taskMeta = msg.taskMeta
        )

        // send task to workers
        val rt = TaskExecutorMessage(
            taskId = newState.taskId,
            taskRetry = newState.taskRetry,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskName = newState.taskName,
            methodName = newState.methodName,
            methodInput = newState.methodInput,
            methodParameterTypes = newState.methodParameterTypes,
            lastTaskAttemptError = null,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(newState.taskId, rt)

        // log events
        val tad = TaskAttemptDispatched(
            taskId = newState.taskId,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry
        )
        taskEngineOutput.sendToTaskEngine(newState.taskId, tad, 0F)

        return newState
    }

    private suspend fun retryTask(oldState: TaskState, msg: RetryTask, messageId: String?): TaskState {
        val newState = oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptId = TaskAttemptId(),
            taskAttemptRetry = TaskAttemptRetry(0),
            taskRetry = oldState.taskRetry + 1,
            taskName = msg.taskName ?: oldState.taskName,
            methodName = msg.methodName ?: oldState.methodName,
            methodInput = msg.methodInput ?: oldState.methodInput,
            methodParameterTypes = msg.methodParameterTypes ?: oldState.methodParameterTypes,
            taskOptions = msg.taskOptions ?: oldState.taskOptions,
            taskMeta = msg.taskMeta ?: oldState.taskMeta
        )

        // send task to workers
        val rt = TaskExecutorMessage(
            taskId = newState.taskId,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry,
            taskName = newState.taskName,
            methodName = newState.methodName,
            methodInput = newState.methodInput,
            methodParameterTypes = newState.methodParameterTypes,
            lastTaskAttemptError = newState.lastTaskAttemptError,
            taskOptions = newState.taskOptions,
            taskMeta = newState.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(newState.taskId, rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = newState.taskId,
            taskAttemptId = newState.taskAttemptId,
            taskAttemptRetry = newState.taskAttemptRetry,
            taskRetry = newState.taskRetry
        )
        taskEngineOutput.sendToTaskEngine(newState.taskId, tad, 0F)

        return newState
    }

    private suspend fun retryTaskAttempt(oldState: TaskState, messageId: String?): TaskState {
        val state = oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptRetry = oldState.taskAttemptRetry + 1
        )

        // send task to workers
        val rt = TaskExecutorMessage(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskRetry = state.taskRetry,
            taskName = state.taskName,
            methodName = state.methodName,
            methodParameterTypes = state.methodParameterTypes,
            methodInput = state.methodInput,
            lastTaskAttemptError = state.lastTaskAttemptError,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        taskEngineOutput.sendToTaskExecutors(state.taskId, rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskRetry = state.taskRetry,
            taskAttemptRetry = state.taskAttemptRetry
        )
        taskEngineOutput.sendToTaskEngine(state.taskId, tar, 0F)

        return state
    }

    private fun taskAttemptStarted(oldState: TaskState, msg: TaskAttemptStarted, messageId: String?): TaskState {
        return oldState.copy(
            messageId = messageId
        )
    }

    private suspend fun taskAttemptCompleted(oldState: TaskState, msg: TaskAttemptCompleted, messageId: String?): TaskState {
        val state = oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.TERMINATED_COMPLETED
        )

        // if this task belongs to a workflow, send back the adhoc message
        state.workflowId?.let {
            taskEngineOutput.sendToWorkflowEngine(
                state.taskId,
                when ("${state.taskName}") {
                    WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                        workflowId = it,
                        workflowTaskId = WorkflowTaskId("${state.taskId}"),
                        workflowTaskOutput = msg.taskOutput.get() as WorkflowTaskOutput
                    )
                    else -> TaskCompletedInWorkflow(
                        workflowId = it,
                        methodRunId = state.methodRunId!!,
                        taskId = state.taskId,
                        taskOutput = msg.taskOutput
                    )
                },
                0F
            )
        }

        // log event
        val tc = TaskCompleted(
            taskId = state.taskId,
            taskName = state.taskName,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        taskEngineOutput.sendToTaskEngine(state.taskId, tc, 0F)

        // delete stored state
        taskStateStorage.deleteState(state.taskId)

        return state
    }

    private suspend fun taskAttemptFailed(oldState: TaskState, msg: TaskAttemptFailed, messageId: String?): TaskState {
        return delayRetryTaskAttempt(
            oldState,
            delay = msg.taskAttemptDelayBeforeRetry,
            error = msg.taskAttemptError,
            messageId
        )
    }

    private suspend fun delayRetryTaskAttempt(
        oldState: TaskState,
        delay: Float?,
        error: TaskAttemptError,
        messageId: String?
    ): TaskState {
        // no retry
        if (delay == null) return oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.RUNNING_ERROR,
            lastTaskAttemptError = error
        )
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(oldState.copy(lastTaskAttemptError = error), messageId)
        // delayed retry
        val state = oldState.copy(
            messageId = messageId,
            taskStatus = TaskStatus.RUNNING_WARNING,
            lastTaskAttemptError = error
        )

        // schedule next attempt
        val tar = RetryTaskAttempt(
            taskId = state.taskId,
            taskRetry = state.taskRetry,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry
        )
        taskEngineOutput.sendToTaskEngine(state.taskId, tar, delay)

        return state
    }
}
