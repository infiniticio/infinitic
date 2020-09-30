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

package io.infinitic.engine.taskManager.engines

import io.infinitic.common.data.interfaces.plus
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.CancelTask
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.messages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.TaskAttemptStarted
import io.infinitic.common.tasks.messages.TaskCanceled
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import io.infinitic.common.tasks.messages.RetryTask
import io.infinitic.common.tasks.messages.RetryTaskAttempt
import io.infinitic.common.tasks.messages.RunTask
import io.infinitic.common.tasks.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.tasks.states.TaskEngineState
import io.infinitic.engine.taskManager.storage.TaskStateStorage

open class TaskEngine(
    protected val storage: TaskStateStorage,
    protected val dispatcher: Dispatcher
) {
    open suspend fun handle(message: ForTaskEngineMessage) {
        // immediately discard messages that are non managed
        when (message) {
            is TaskAttemptDispatched -> return
            is TaskAttemptStarted -> return
            is TaskCompleted -> return
            is TaskCanceled -> return
            else -> Unit
        }

        // get current state
        val oldState = storage.getTaskEngineState(message.taskId)

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
            storage.updateTaskEngineState(message.taskId, newState, oldState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val tsc = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = TaskName("${newState.taskName}::${newState.taskMethod.methodName}"),
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            dispatcher.toMonitoringPerName(tsc)
        }
    }

    private suspend fun cancelTask(oldState: TaskEngineState, msg: CancelTask): TaskEngineState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_CANCELED)

        // log event
        val tad = TaskCanceled(
            taskId = state.taskId,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        dispatcher.toTaskEngine(tad)

        // Delete stored state
        storage.deleteTaskEngineState(state.taskId)

        return state
    }

    private suspend fun dispatchTask(msg: DispatchTask): TaskEngineState {
        // init a state
        val state = TaskEngineState(
            taskId = msg.taskId,
            taskName = msg.taskName,
            taskMethod = msg.taskMethod,
            taskInput = msg.taskInput,
            taskAttemptId = TaskAttemptId(),
            taskStatus = TaskStatus.RUNNING_OK,
            taskOptions = msg.taskOptions,
            taskMeta = msg.taskMeta
        )

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskMethod = state.taskMethod,
            taskInput = state.taskInput,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        dispatcher.toWorkers(rt)

        // log events
        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex
        )
        dispatcher.toTaskEngine(tad)

        return state
    }

    private suspend fun retryTask(oldState: TaskEngineState, msg: RetryTask): TaskEngineState {
        val state = oldState.copy(
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptId = TaskAttemptId(),
            taskAttemptRetry = TaskAttemptRetry(0),
            taskAttemptIndex = oldState.taskAttemptIndex + 1,
            taskName = msg.taskName ?: oldState.taskName,
            taskInput = msg.taskInput ?: oldState.taskInput,
            taskOptions = msg.taskOptions ?: oldState.taskOptions,
            taskMeta = msg.taskMeta ?: oldState.taskMeta
        )

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskMethod = state.taskMethod,
            taskInput = state.taskInput,
            taskMeta = state.taskMeta,
            taskOptions = state.taskOptions
        )
        dispatcher.toWorkers(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex
        )
        dispatcher.toTaskEngine(tad)

        return state
    }

    private suspend fun retryTaskAttempt(oldState: TaskEngineState): TaskEngineState {
        val state = oldState.copy(
            taskStatus = TaskStatus.RUNNING_WARNING,
            taskAttemptRetry = oldState.taskAttemptRetry + 1
        )

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskMethod = state.taskMethod,
            taskInput = state.taskInput,
            taskOptions = state.taskOptions,
            taskMeta = state.taskMeta
        )
        dispatcher.toWorkers(rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskAttemptRetry = state.taskAttemptRetry
        )
        dispatcher.toTaskEngine(tar)

        return state
    }

    private suspend fun taskAttemptCompleted(oldState: TaskEngineState, msg: TaskAttemptCompleted): TaskEngineState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_COMPLETED)

        // log event
        val tc = TaskCompleted(
            taskId = state.taskId,
            taskName = state.taskName,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        dispatcher.toTaskEngine(tc)

        // Delete stored state
        storage.deleteTaskEngineState(state.taskId)

        return state
    }

    private suspend fun taskAttemptFailed(oldState: TaskEngineState, msg: TaskAttemptFailed): TaskEngineState {
        return delayRetryTaskAttempt(oldState, delay = msg.taskAttemptDelayBeforeRetry)
    }

    private suspend fun delayRetryTaskAttempt(oldState: TaskEngineState, delay: Float?): TaskEngineState {
        // no retry
        if (delay == null) return oldState.copy(taskStatus = TaskStatus.RUNNING_ERROR)
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(oldState)
        // delayed retry
        val state = oldState.copy(taskStatus = TaskStatus.RUNNING_WARNING)

        // schedule next attempt
        val tar = RetryTaskAttempt(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptRetry = state.taskAttemptRetry,
            taskAttemptIndex = state.taskAttemptIndex
        )
        dispatcher.toTaskEngine(tar, after = delay)

        return state
    }
}
