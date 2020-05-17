package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptFailingMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.workflowengine.interfaces.LoggerInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted

class TaskEngine {
    lateinit var stater: TaskStaterInterface
    lateinit var dispatcher: TaskEngineDispatcherInterface
    lateinit var logger: LoggerInterface

    fun handle(msg: TaskMessageInterface) {
        // get associated state
        var state = stater.getState(msg.getStateId())
        if (state == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is DispatchTask) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", msg)
                return
            }
            // init a state
            state = TaskState(
                taskId = msg.taskId,
                taskName = msg.taskName,
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
            if (msg is TaskAttemptMessageInterface) {
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
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is CancelTask -> cancelTask(state, msg)
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
    }

    private fun cancelTask(state: TaskState, msg: CancelTask) {
        // update and save state
        stater.deleteState(msg.getStateId())

        // log event
        val tad = TaskCanceled(
            taskId = msg.taskId
        )
        dispatcher.dispatch(tad)
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
        dispatcher.dispatch(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = msg.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex
        )
        dispatcher.dispatch(tad)

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
        dispatcher.dispatch(rt)

        // log event
        val tad = TaskAttemptDispatched(
            taskId = rt.taskId,
            taskAttemptId = rt.taskAttemptId,
            taskAttemptIndex = rt.taskAttemptIndex
        )
        dispatcher.dispatch(tad)

        // update state
        state.taskAttemptId = rt.taskAttemptId
        state.taskAttemptIndex = rt.taskAttemptIndex
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
        dispatcher.dispatch(rt)

        // log event
        val tar = TaskAttemptDispatched(
            taskId = state.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = newIndex
        )
        dispatcher.dispatch(tar)

        // update state
        state.taskAttemptIndex = newIndex
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
            dispatcher.dispatch(tc)
        }
        // delete state
        stater.deleteState(msg.getStateId())
    }

    private fun taskAttemptFailed(state: TaskState, msg: TaskAttemptFailed) {
        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun triggerDelayedRetry(state: TaskState, msg: TaskAttemptFailingMessageInterface) {
        if (msg.taskAttemptDelayBeforeRetry == null) {
            return
        }
        val delay = msg.taskAttemptDelayBeforeRetry!!
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
            dispatcher.dispatch(tar, after = delay)
        }
    }
}
