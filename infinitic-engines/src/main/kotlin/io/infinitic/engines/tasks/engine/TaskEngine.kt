// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.tasks.engine

import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.monitoringPerName.messages.TaskStatusUpdated
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.CancelTask
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.messages.RetryTask
import io.infinitic.common.tasks.messages.RetryTaskAttempt
import io.infinitic.common.tasks.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.messages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.TaskAttemptStarted
import io.infinitic.common.tasks.messages.TaskCanceled
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.tasks.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.tasks.state.TaskState
import io.infinitic.common.workers.messages.RunTask
import io.infinitic.engines.tasks.storage.TaskStateStorage

open class TaskEngine(
    protected val storage: TaskStateStorage,
    protected val sendToTaskEngine: SendToTaskEngine,
    protected val sendToMonitoringPerName: SendToMonitoringPerName,
    protected val sendToWorkers: SendToWorkers
) {
    open suspend fun handle(message: TaskEngineMessage) {
        // immediately discard messages that are non managed
        when (message) {
            is TaskAttemptDispatched -> return
            is TaskAttemptStarted -> return
            is TaskCompleted -> return
            is TaskCanceled -> return
            else -> Unit
        }

        // get current state
        val oldState = storage.getState(message.taskId)

        if (oldState != null) {
            // discard message (except TaskAttemptCompleted) if state has already evolved
            if (message is TaskAttemptMessage && message !is TaskAttemptCompleted) {
                if (oldState.taskAttemptId != message.taskAttemptId) return
                if (oldState.taskAttemptRetry != message.taskAttemptRetry) return
            }
        } else {
            // discard message if task is already completed
            if (message !is DispatchTask) return
        }

        val newState =
            if (oldState == null)
                dispatchTask(message as DispatchTask)
            else when (message) {
                is CancelTask -> cancelTask(oldState, message)
                is RetryTask -> retryTask(oldState, message)
                is RetryTaskAttempt -> retryTaskAttempt(oldState)
                is TaskAttemptFailed -> taskAttemptFailed(oldState, message)
                is TaskAttemptCompleted -> taskAttemptCompleted(oldState, message)
                else -> throw Exception("Unknown EngineMessage: $message")
            }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.taskStatus.isTerminated) {
            storage.updateState(message.taskId, newState, oldState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val tsc = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = TaskName("${newState.taskName}::${newState.methodName}"),
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            sendToMonitoringPerName(tsc)
        }
    }

    private suspend fun cancelTask(oldState: TaskState, msg: CancelTask): TaskState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_CANCELED)

        // log event
        val tad = TaskCanceled(
            taskId = state.taskId,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        sendToTaskEngine(tad, 0F)

        // Delete stored state
        storage.deleteState(state.taskId)

        return state
    }

    private suspend fun dispatchTask(msg: DispatchTask): TaskState {
        // init a state
        val state = TaskState(
            taskId = msg.taskId,
            taskName = msg.taskName,
            methodName = msg.methodName,
            methodParameterTypes = msg.methodParameterTypes,
            methodInput = msg.methodInput,
            taskAttemptId = TaskAttemptId(),
            taskStatus = TaskStatus.RUNNING_OK,
            taskOptions = msg.taskOptions,
            taskMeta = msg.taskMeta
        )

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskRetry = state.taskRetry,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskName = state.taskName,
            methodName = state.methodName,
            methodInput = state.methodInput,
            methodParameterTypes = state.methodParameterTypes,
            lastTaskAttemptError = null,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        sendToWorkers(rt)

        // log events
        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskRetry = state.taskRetry
        )
        sendToTaskEngine(tad, 0F)

        return state
    }

    private suspend fun retryTask(oldState: TaskState, msg: RetryTask): TaskState {
        val state = oldState.copy(
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
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskRetry = state.taskRetry,
            taskName = state.taskName,
            methodName = state.methodName,
            methodInput = state.methodInput,
            methodParameterTypes = state.methodParameterTypes,
            lastTaskAttemptError = state.lastTaskAttemptError,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        sendToWorkers(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskRetry = state.taskRetry
        )
        sendToTaskEngine(tad, 0F)

        return state
    }

    private suspend fun retryTaskAttempt(oldState: TaskState): TaskState {
        val state = oldState.copy(
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptRetry = oldState.taskAttemptRetry + 1
        )

        // send task to workers
        val rt = RunTask(
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
        sendToWorkers(rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskRetry = state.taskRetry,
            taskAttemptRetry = state.taskAttemptRetry
        )
        sendToTaskEngine(tar, 0F)

        return state
    }

    private suspend fun taskAttemptCompleted(oldState: TaskState, msg: TaskAttemptCompleted): TaskState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_COMPLETED)

        // log event
        val tc = TaskCompleted(
            taskId = state.taskId,
            taskName = state.taskName,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        sendToTaskEngine(tc, 0F)

        // delete stored state
        storage.deleteState(state.taskId)

        return state
    }

    private suspend fun taskAttemptFailed(oldState: TaskState, msg: TaskAttemptFailed): TaskState {
        return delayRetryTaskAttempt(
            oldState,
            delay = msg.taskAttemptDelayBeforeRetry,
            error = msg.taskAttemptError
        )
    }

    private suspend fun delayRetryTaskAttempt(
        oldState: TaskState,
        delay: Float?,
        error: TaskAttemptError
    ): TaskState {
        // no retry
        if (delay == null) return oldState.copy(
            taskStatus = TaskStatus.RUNNING_ERROR,
            lastTaskAttemptError = error
        )
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(oldState.copy(lastTaskAttemptError = error))
        // delayed retry
        val state = oldState.copy(
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
        sendToTaskEngine(tar, delay)

        return state
    }
}
