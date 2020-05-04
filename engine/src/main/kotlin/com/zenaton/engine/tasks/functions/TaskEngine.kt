package com.zenaton.engine.tasks.functions

import com.zenaton.engine.interfaces.LoggerInterface
import com.zenaton.engine.interfaces.StaterInterface
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.tasks.data.TaskState
import com.zenaton.engine.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.tasks.messages.TaskMessageInterface
import com.zenaton.engine.workflows.messages.TaskCompleted

class TaskEngine(
    private val stater: StaterInterface<TaskState>,
    private val dispatcher: TaskEngineDispatcherInterface,
    private val logger: LoggerInterface
) {
    fun handle(msg: TaskMessageInterface) {
        // timestamp the message
        msg.receivedAt = DateTime()
        // get associated state
        var state = stater.getState(msg.getKey())
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
            // a non-null state with TaskDispatched should mean that this message has been replicated
            if (msg is TaskDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is TaskAttemptCompleted -> completeTaskAttempt(state, msg)
            is TaskAttemptFailed -> failTaskAttempt(state, msg)
            is TaskAttemptStarted -> startTaskAttempt(state, msg)
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
        stater.deleteState(msg.getKey())
    }

    private fun failTaskAttempt(state: TaskState, msg: TaskAttemptFailed) {
        if (state.taskAttemptId != msg.taskAttemptId) {
            logger.warn("Inconsistent taskAttemptId in message:%s and State:%s(Can happen if this task has been manually retried)", msg, state)
            return
        }
        if (state.taskAttemptIndex != msg.taskAttemptIndex) {
            logger.warn("Inconsistent taskAttemptIndex in message:%s and State:%s(Can happen if this task has had timeout)", msg, state)
            return
        }
        val tad = TaskAttemptDispatched(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = 1 + msg.taskAttemptIndex,
            taskName = state.taskName,
            taskData = state.taskData
        )
        dispatcher.dispatch(tad)

        TODO("Must implement delay between retry")
    }

    private fun startTaskAttempt(state: TaskState, msg: TaskAttemptStarted) {
        TODO("Must implement timeout")
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
        stater.createState(msg.getKey(), state)
    }
}
