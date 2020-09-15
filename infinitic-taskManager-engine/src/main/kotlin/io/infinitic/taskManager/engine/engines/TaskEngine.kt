package io.infinitic.taskManager.engine.engines

import io.infinitic.common.data.interfaces.plus
import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.engine.dispatcher.EngineDispatcher
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.TaskAttemptCompleted
import io.infinitic.taskManager.common.messages.TaskAttemptDispatched
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.messages.TaskAttemptStarted
import io.infinitic.taskManager.common.messages.TaskCanceled
import io.infinitic.taskManager.common.messages.TaskCompleted
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.common.messages.RetryTask
import io.infinitic.taskManager.common.messages.RetryTaskAttempt
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.messages.interfaces.TaskAttemptMessage
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.engine.storage.TaskStateStorage

class TaskEngine(
    val storage: TaskStateStorage,
    val dispatcher: EngineDispatcher
) {
    suspend fun handle(message: ForTaskEngineMessage) {
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
                taskName = newState.taskName,
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
