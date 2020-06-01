package com.zenaton.taskManager.engine

import com.zenaton.taskManager.data.TaskAttemptId
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import com.zenaton.taskManager.messages.TaskAttemptMessage
import com.zenaton.taskManager.monitoring.perName.TaskStatusUpdated
import com.zenaton.taskManager.workers.RunTask
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as TaskCompletedInWorkflow

class Engine {
    lateinit var taskDispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcherInterface
    lateinit var storage: EngineStorage
    lateinit var logger: Logger

    fun handle(msg: EngineMessage) {
        // get associated state
        val oldState = storage.getState(msg.taskId)
        var newState = oldState?.copy()

        if (newState == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is DispatchTask) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", msg, null)
                return
            }
            // init a state
            newState = EngineState(
                taskId = msg.taskId,
                taskName = msg.taskName,
                taskData = msg.taskData,
                workflowId = msg.workflowId,
                taskAttemptId = TaskAttemptId(),
                taskAttemptIndex = 0,
                taskStatus = TaskStatus.RUNNING_OK
            )
        } else {
            // this should never happen
            if (newState.taskId != msg.taskId) {
                logger.error("Inconsistent taskId in message:%s and State:%s)", msg, newState)
                return
            }
            // a non-null state with TaskDispatched should mean that this message has been replicated
            if (msg is DispatchTask) {
                logger.error("Already existing state:%s for message:%s", msg, newState)
                return
            }
            // check taskAttemptId and taskAttemptIndex consistency
            if (msg is TaskAttemptMessage && msg !is TaskAttemptCompleted) {
                if (newState.taskAttemptId != msg.taskAttemptId) {
                    logger.warn("Inconsistent taskAttemptId in message: (Can happen if the task has been manually retried)%s and State:%s", msg, newState)
                    return
                }
                if (newState.taskAttemptIndex != msg.taskAttemptIndex) {
                    logger.warn("Inconsistent taskAttemptIndex in message: (Can happen if this task has had timeout)%s and State:%s", msg, newState)
                    return
                }
            }
        }

        when (msg) {
            is CancelTask            -> cancelTask(newState, msg)
            is DispatchTask          -> dispatchTask(newState, msg)
            is RetryTask             -> retryTask(newState, msg)
            is RetryTaskAttempt      -> retryTaskAttempt(newState, msg)
            is TaskAttemptCompleted  -> taskAttemptCompleted(newState, msg)
            is TaskAttemptDispatched -> Unit
            is TaskAttemptFailed     -> taskAttemptFailed(newState, msg)
            is TaskAttemptStarted    -> Unit
            is TaskCanceled          -> Unit
            is TaskCompleted         -> Unit
            is TaskDispatched        -> Unit
        }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.taskStatus.isTerminated) {
            storage.updateState(msg.taskId, newState, oldState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.taskStatus != newState.taskStatus) {
            val tsc = TaskStatusUpdated(
                taskId = newState.taskId,
                taskName = newState.taskName,
                oldStatus = oldState?.taskStatus,
                newStatus = newState.taskStatus
            )

            taskDispatcher.dispatch(tsc)
        }
    }

    private fun cancelTask(state: EngineState, msg: CancelTask) {
        state.taskStatus = TaskStatus.TERMINATED_CANCELED

        // log event
        val tad = TaskCanceled(
            taskId = msg.taskId
        )
        taskDispatcher.dispatch(tad)

        // Delete stored state
        storage.deleteState(state.taskId)
    }

    private fun dispatchTask(state: EngineState, msg: DispatchTask) {
        state.taskStatus = TaskStatus.RUNNING_OK

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        taskDispatcher.dispatch(rt)

        // log events
        val td = TaskDispatched(
            taskId = state.taskId
        )
        taskDispatcher.dispatch(td)

        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex
        )
        taskDispatcher.dispatch(tad)
    }

    private fun retryTask(state: EngineState, msg: RetryTask) {
        state.taskStatus = TaskStatus.RUNNING_WARNING
        state.taskAttemptId = TaskAttemptId()
        state.taskAttemptIndex = 0

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex
        )
        taskDispatcher.dispatch(tad)
    }

    private fun retryTaskAttempt(state: EngineState, msg: EngineMessage) {
        state.taskStatus = TaskStatus.RUNNING_WARNING
        state.taskAttemptIndex = state.taskAttemptIndex + 1

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex
        )
        taskDispatcher.dispatch(tar)
    }

    private fun taskAttemptCompleted(state: EngineState, msg: TaskAttemptCompleted) {
        state.taskStatus = TaskStatus.TERMINATED_COMPLETED

        // if this task belongs to a workflow
        if (state.workflowId != null) {
            val tciw = TaskCompletedInWorkflow(
                workflowId = state.workflowId,
                taskId = state.taskId,
                taskOutput = msg.taskOutput
            )
            workflowDispatcher.dispatch(tciw)
        }

        // log event
        val tc = TaskCompleted(
            taskId = state.taskId,
            taskOutput = msg.taskOutput
        )
        taskDispatcher.dispatch(tc)

        // Delete stored state
        storage.deleteState(state.taskId)
    }

    private fun taskAttemptFailed(state: EngineState, msg: TaskAttemptFailed) {
        state.taskStatus = TaskStatus.RUNNING_ERROR

        delayRetryTaskAttempt(state = state, msg = msg, delay = msg.taskAttemptDelayBeforeRetry)
    }

    private fun delayRetryTaskAttempt(state: EngineState, msg: EngineMessage, delay: Float?) {
        // no retry
        if (delay == null) return
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(state, msg)
        // delayed retry
        if (delay > 0f) {
            state.taskStatus = TaskStatus.RUNNING_WARNING

            // schedule next attempt
            val tar = RetryTaskAttempt(
                taskId = state.taskId,
                taskAttemptId = state.taskAttemptId,
                taskAttemptIndex = state.taskAttemptIndex
            )
            taskDispatcher.dispatch(tar, after = delay)
        }
    }
}
