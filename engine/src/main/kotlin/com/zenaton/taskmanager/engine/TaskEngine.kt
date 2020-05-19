package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcherInterface
import com.zenaton.taskmanager.logger.TaskLoggerInterface
import com.zenaton.taskmanager.messages.TaskAttemptFailingMessageInterface
import com.zenaton.taskmanager.messages.TaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.RunTask
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.messages.events.TaskStatusUpdated
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.taskmanager.state.TaskStaterInterface
import com.zenaton.taskmanager.state.TaskStatusTransition
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted

class TaskEngine {
    lateinit var taskDispatcher: TaskDispatcherInterface
    lateinit var workflowDispatcher: WorkflowDispatcherInterface
    lateinit var stater: TaskStaterInterface
    lateinit var logger: TaskLoggerInterface

    fun handle(msg: TaskMessageInterface) {
        // get associated state
        var state = stater.getState(msg.getStateId())
        var stateTransition: TaskStatusTransition
        if (state == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is DispatchTask) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", msg, null)
                return
            }

            // init a state transition for the new task
            stateTransition = TaskStatusTransition.createNew()

            // init a state
            state = TaskState(
                taskId = msg.taskId,
                taskName = msg.taskName,
                taskStatus = TaskStatus.OK,
                taskData = msg.taskData,
                taskAttemptId = TaskAttemptId(),
                taskAttemptIndex = 0,
                workflowId = msg.workflowId
            )
        } else {
            // this should never happen
            if (state.taskId != msg.taskId) {
                logger.error("Inconsistent taskId in message:%s and State:%s)", msg, state)
                return
            }
            if (msg is TaskAttemptMessageInterface && msg !is TaskAttemptCompleted) {
                if (state.taskAttemptId != msg.taskAttemptId) {
                    logger.warn("Inconsistent taskAttemptId in message: (Can happen if the task has been manually retried)%s and State:%s", msg, state)
                    return
                }
                if (state.taskAttemptIndex != msg.taskAttemptIndex) {
                    logger.warn("Inconsistent taskAttemptIndex in message: (Can happen if this task has had timeout)%s and State:%s", msg, state)
                    return
                }
            }
            // a non-null state with TaskDispatched should mean that this message has been replicated
            if (msg is DispatchTask) {
                logger.error("Already existing state:%s for message:%s", msg, state)
                return
            }

            // init a state transition for the already existing task
            stateTransition = TaskStatusTransition.createFromState(state)
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
            else -> throw Exception("Unknown Message $msg")
        }

        // capture the new state of the task and notify change of status
        stateTransition.newStatus = state.taskStatus
        notifyTaskStatusUpdated(state, stateTransition)
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
            taskId = msg.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = msg.taskName,
            taskData = msg.taskData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = rt.taskId,
            taskAttemptId = rt.taskAttemptId,
            taskAttemptIndex = rt.taskAttemptIndex
        )
        taskDispatcher.dispatch(tad)

        // update and save state
        stater.createState(msg.getStateId(), state)
    }

    private fun retryTask(state: TaskState, msg: RetryTask) {
        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = TaskAttemptId(),
            taskAttemptIndex = 0,
            taskName = state.taskName,
            taskData = state.taskData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = rt.taskId,
            taskAttemptId = rt.taskAttemptId,
            taskAttemptIndex = rt.taskAttemptIndex
        )
        taskDispatcher.dispatch(tad)

        // update state
        state.taskAttemptId = rt.taskAttemptId
        state.taskAttemptIndex = rt.taskAttemptIndex
        state.taskStatus = TaskStatus.WARNING
        stater.updateState(msg.getStateId(), state)
    }

    private fun retryTaskAttempt(state: TaskState, msg: TaskMessageInterface) {
        val newIndex = state.taskAttemptIndex + 1

        // send task to workers
        val rt = RunTask(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = newIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = newIndex
        )
        taskDispatcher.dispatch(tar)

        // update state
        state.taskAttemptIndex = newIndex
        state.taskStatus = TaskStatus.WARNING
        stater.updateState(msg.getStateId(), state)
    }

    private fun taskAttemptCompleted(state: TaskState, msg: TaskAttemptCompleted) {
        // if this task belongs to a workflow
        if (state.workflowId != null) {
            val tc = TaskCompleted(
                workflowId = state.workflowId,
                taskId = state.taskId,
                taskOutput = msg.taskOutput
            )
            workflowDispatcher.dispatch(tc)
        }
        // need to set the state to null for the status updated notification
        state.taskStatus = null
        // delete state
        stater.deleteState(msg.getStateId())
    }

    private fun taskAttemptFailed(state: TaskState, msg: TaskAttemptFailed) {
        state.taskStatus = TaskStatus.ERROR

        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun triggerDelayedRetry(state: TaskState, msg: TaskAttemptFailingMessageInterface) {
        val delay = msg.taskAttemptDelayBeforeRetry ?: return
        if (delay <= 0f) {
            return retryTaskAttempt(state, msg)
        }
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

    private fun notifyTaskStatusUpdated(state: TaskState, transition: TaskStatusTransition) {
        if (!transition.isVoid) {
            return
        }

        val tsc = TaskStatusUpdated(
            taskId = state.taskId,
            taskName = state.taskName,
            oldStatus = transition.oldStatus,
            newStatus = transition.newStatus
        )

        taskDispatcher.dispatch(tsc)
    }
}
