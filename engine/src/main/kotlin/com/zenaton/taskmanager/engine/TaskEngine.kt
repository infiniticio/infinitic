package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.messages.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.TaskAttemptFailed
import com.zenaton.taskmanager.messages.TaskAttemptRetried
import com.zenaton.taskmanager.messages.TaskAttemptStarted
import com.zenaton.taskmanager.messages.TaskAttemptTimeout
import com.zenaton.taskmanager.messages.TaskDispatched
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptFailingMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.workflowengine.interfaces.LoggerInterface
import com.zenaton.workflowengine.interfaces.StaterInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted

class TaskEngine(
    private val stater: StaterInterface<TaskState>,
    private val dispatcher: TaskEngineDispatcherInterface,
    private val logger: LoggerInterface
) {
    fun handle(msg: TaskMessageInterface) {
        // get associated state
        var state = stater.getState(msg.getStateId())
        if (state == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is TaskDispatched) {
                logger.warn("No state found for message:%s(It's normal if this task is already terminated)", msg)
                return
            }
            // init a state
            state = TaskState(
                taskId = msg.taskId,
                taskName = msg.taskName,
                taskData = msg.taskData,
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
            if (msg is TaskDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is TaskAttemptCompleted -> completeTaskAttempt(state, msg)
            is TaskAttemptFailed -> failTaskAttempt(state, msg)
            is TaskAttemptRetried -> retryTaskAttempt(state, msg)
            is TaskAttemptStarted -> startTaskAttempt(state, msg)
            is TaskAttemptTimeout -> timeoutTaskAttempt(state, msg)
            is TaskDispatched -> dispatchTask(state, msg)
        }
    }

    private fun completeTaskAttempt(state: TaskState, msg: TaskAttemptCompleted) {
        // if this task belongs to a workflow
        if (state.workflowId != null) {
            val tc = TaskCompleted(
                workflowId = state.workflowId,
                taskId = msg.taskId,
                taskOutput = msg.taskOutput
            )
            dispatcher.dispatch(tc)
        }
        // delete state
        stater.deleteState(msg.getStateId())
    }

    private fun failTaskAttempt(state: TaskState, msg: TaskAttemptFailed) {
        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun retryTaskAttempt(state: TaskState, msg: TaskAttemptRetried) {
        val tad = TaskAttemptDispatched(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        dispatcher.dispatch(tad)
    }

    private fun startTaskAttempt(state: TaskState, msg: TaskAttemptStarted) {
        if (msg.taskAttemptDelayBeforeTimeout > 0f) {
            val tad = TaskAttemptTimeout(
                taskId = msg.taskId,
                taskAttemptId = msg.taskAttemptId,
                taskAttemptIndex = msg.taskAttemptIndex,
                taskAttemptDelayBeforeRetry = msg.taskAttemptDelayBeforeRetry
            )
            dispatcher.dispatch(tad, after = msg.taskAttemptDelayBeforeTimeout)
        }
    }

    private fun timeoutTaskAttempt(state: TaskState, msg: TaskAttemptTimeout) {
        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun dispatchTask(state: TaskState, msg: TaskDispatched) {
        // dispatch a task attempt
        val tad = TaskAttemptDispatched(
            taskId = msg.taskId,
            taskAttemptId = state.taskAttemptId,
            taskAttemptIndex = state.taskAttemptIndex,
            taskName = msg.taskName,
            taskData = msg.taskData
        )
        dispatcher.dispatch(tad)
        // update and save state
        stater.createState(msg.getStateId(), state)
    }

    private fun triggerDelayedRetry(state: TaskState, msg: TaskAttemptFailingMessageInterface) {
        if (msg.taskAttemptDelayBeforeRetry >= 0f) {
            val newIndex = 1 + msg.taskAttemptIndex
            // schedule next attempt
            val tar = TaskAttemptRetried(
                taskId = state.taskId,
                taskAttemptId = state.taskAttemptId,
                taskAttemptIndex = newIndex
            )
            if (msg.taskAttemptDelayBeforeRetry <= 0f) {
                retryTaskAttempt(state, tar)
            } else {
                dispatcher.dispatch(tar, after = msg.taskAttemptDelayBeforeRetry)
            }
            // update state
            state.taskAttemptIndex = newIndex
            stater.updateState(msg.getStateId(), state)
        }
    }
}
