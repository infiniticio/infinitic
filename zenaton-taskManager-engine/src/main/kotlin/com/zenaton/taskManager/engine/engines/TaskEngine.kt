package com.zenaton.taskManager.engine.engines

import com.zenaton.common.data.interfaces.plus
import com.zenaton.taskManager.common.data.TaskAttemptId
import com.zenaton.taskManager.common.data.TaskAttemptRetry
import com.zenaton.taskManager.common.data.TaskStatus
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.common.messages.CancelTask
import com.zenaton.taskManager.common.messages.DispatchTask
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import com.zenaton.taskManager.common.messages.TaskAttemptCompleted
import com.zenaton.taskManager.common.messages.TaskAttemptDispatched
import com.zenaton.taskManager.common.messages.TaskAttemptFailed
import com.zenaton.taskManager.common.messages.TaskAttemptStarted
import com.zenaton.taskManager.common.messages.TaskCanceled
import com.zenaton.taskManager.common.messages.TaskCompleted
import com.zenaton.taskManager.common.messages.TaskStatusUpdated
import com.zenaton.taskManager.common.messages.RetryTask
import com.zenaton.taskManager.common.messages.RetryTaskAttempt
import com.zenaton.taskManager.common.messages.RunTask
import com.zenaton.taskManager.common.messages.interfaces.TaskAttemptMessage
import com.zenaton.taskManager.common.states.TaskEngineState
import com.zenaton.taskManager.engine.storages.TaskEngineStateStorage
import org.slf4j.Logger

class TaskEngine {
    lateinit var logger: Logger
    lateinit var storage: TaskEngineStateStorage
    lateinit var dispatcher: Dispatcher

    fun handle(message: ForTaskEngineMessage) {
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
                taskName = newState.taskName,
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            dispatcher.toMonitoringPerName(tsc)
        }
    }

    private fun cancelTask(oldState: TaskEngineState, msg: CancelTask): TaskEngineState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_CANCELED)

        // log event
        val tad = TaskCanceled(
            taskId = state.taskId,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        dispatcher.toTaskEngine(tad)

        // Delete stored state
        storage.deleteState(state.taskId)

        return state
    }

    private fun dispatchTask(msg: DispatchTask): TaskEngineState {
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

    private fun retryTask(oldState: TaskEngineState, msg: RetryTask): TaskEngineState {
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

    private fun retryTaskAttempt(oldState: TaskEngineState): TaskEngineState {
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

    private fun taskAttemptCompleted(oldState: TaskEngineState, msg: TaskAttemptCompleted): TaskEngineState {
        val state = oldState.copy(taskStatus = TaskStatus.TERMINATED_COMPLETED)

        // log event
        val tc = TaskCompleted(
            taskId = state.taskId,
            taskOutput = msg.taskOutput,
            taskMeta = state.taskMeta
        )
        dispatcher.toTaskEngine(tc)

        // Delete stored state
        storage.deleteState(state.taskId)

        return state
    }

    private fun taskAttemptFailed(oldState: TaskEngineState, msg: TaskAttemptFailed): TaskEngineState {
        return delayRetryTaskAttempt(oldState, delay = msg.taskAttemptDelayBeforeRetry)
    }

    private fun delayRetryTaskAttempt(oldState: TaskEngineState, delay: Float?): TaskEngineState {
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
