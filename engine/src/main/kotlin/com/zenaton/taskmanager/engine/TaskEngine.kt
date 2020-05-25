package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.messages.engine.CancelTask
import com.zenaton.taskmanager.messages.engine.DispatchTask
import com.zenaton.taskmanager.messages.engine.RetryTask
import com.zenaton.taskmanager.messages.engine.RetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.TaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.TaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.TaskCanceled
import com.zenaton.taskmanager.messages.engine.TaskCompleted
import com.zenaton.taskmanager.messages.engine.TaskDispatched
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.RunTask
import com.zenaton.taskmanager.stater.TaskStaterInterface
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as TaskCompletedInWorkflow

class TaskEngine {
    lateinit var taskDispatcher: TaskDispatcher
    lateinit var workflowDispatcher: WorkflowDispatcherInterface
    lateinit var stater: TaskStaterInterface
    lateinit var logger: TaskLogger

    fun handle(msg: TaskEngineMessage) {
        var oldStatus: TaskStatus? = null

        // get associated state
        var state = stater.getState(msg.getStateId())
        if (state == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is DispatchTask) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", msg, null)
                return
            }
            // init a state
            state = TaskState(
                taskId = msg.taskId,
                taskName = msg.taskName,
                taskData = msg.taskData,
                workflowId = msg.workflowId,
                taskAttemptId = TaskAttemptId(),
                taskAttemptIndex = 0,
                taskStatus = TaskStatus.OK
            )
        } else {
            // this should never happen
            if (state.taskId != msg.taskId) {
                logger.error("Inconsistent taskId in message:%s and State:%s)", msg, state)
                return
            }
            // a non-null state with TaskDispatched should mean that this message has been replicated
            if (msg is DispatchTask) {
                logger.error("Already existing state:%s for message:%s", msg, state)
                return
            }
            // check taskAttemptId and taskAttemptIndex consistency
            if (msg is TaskAttemptMessage && msg !is TaskAttemptCompleted) {
                if (state.taskAttemptId != msg.taskAttemptId) {
                    logger.warn("Inconsistent taskAttemptId in message: (Can happen if the task has been manually retried)%s and State:%s", msg, state)
                    return
                }
                if (state.taskAttemptIndex != msg.taskAttemptIndex) {
                    logger.warn("Inconsistent taskAttemptIndex in message: (Can happen if this task has had timeout)%s and State:%s", msg, state)
                    return
                }
            }
            // set current status
            oldStatus = state.taskStatus
        }

        when (msg) {
            is CancelTask -> cancelTask(msg)
            is DispatchTask -> dispatchTask(state, msg)
            is RetryTask -> retryTask(state, msg)
            is RetryTaskAttempt -> retryTaskAttempt(state, msg)
            is TaskAttemptCompleted -> taskAttemptCompleted(state, msg)
            is TaskAttemptDispatched -> Unit
            is TaskAttemptFailed -> taskAttemptFailed(state, msg)
            is TaskAttemptStarted -> Unit
            is TaskCanceled -> Unit
            is TaskCompleted -> Unit
            is TaskDispatched -> Unit
        }

        // capture the new state of the task and notify change of status
        val newStatus = when (msg) {
            is CancelTask, is TaskAttemptCompleted -> null
            else -> state.taskStatus
        }

        if (oldStatus != newStatus) {
            val tsc = TaskStatusUpdated(
                taskId = state.taskId,
                taskName = state.taskName,
                oldStatus = oldStatus,
                newStatus = newStatus
            )

            taskDispatcher.dispatch(tsc)
        }
    }

    private fun cancelTask(msg: CancelTask) {
        // update and save state
        stater.deleteState(msg.getStateId())

        // log event
        val tad = TaskCanceled(
            taskId = msg.taskId
        )
        taskDispatcher.dispatch(tad)
    }

    private fun dispatchTask(state: TaskState, msg: DispatchTask) {
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

        // update and save state
        stater.createState(msg.getStateId(), state)
    }

    private fun retryTask(state: TaskState, msg: RetryTask) {
        state.taskAttemptId = TaskAttemptId()
        state.taskAttemptIndex = 0
        state.taskStatus = TaskStatus.WARNING

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

        // update state
        stater.updateState(msg.getStateId(), state)
    }

    private fun retryTaskAttempt(state: TaskState, msg: TaskEngineMessage) {
        state.taskAttemptIndex = state.taskAttemptIndex + 1
        state.taskStatus = TaskStatus.WARNING

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

        // update state
        stater.updateState(msg.getStateId(), state)
    }

    private fun taskAttemptCompleted(state: TaskState, msg: TaskAttemptCompleted) {
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

        // delete state
        stater.deleteState(msg.getStateId())
    }

    private fun taskAttemptFailed(state: TaskState, msg: TaskAttemptFailed) {
        state.taskStatus = TaskStatus.ERROR

        delayRetryTaskAttempt(state = state, msg = msg, delay = msg.taskAttemptDelayBeforeRetry)
    }

    private fun delayRetryTaskAttempt(state: TaskState, msg: TaskEngineMessage, delay: Float?) {
        // no retry
        if (delay == null) return
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(state, msg)
        // delayed retry
        if (delay > 0f) {
            // schedule next attempt
            val tar = RetryTaskAttempt(
                taskId = state.taskId,
                taskAttemptId = state.taskAttemptId,
                taskAttemptIndex = state.taskAttemptIndex
            )
            taskDispatcher.dispatch(tar, after = delay)

            state.taskStatus = TaskStatus.WARNING
            stater.updateState(msg.getStateId(), state)
        }
    }
}
