package com.zenaton.engine.topics.tasks

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskState
import com.zenaton.engine.topics.LoggerInterface
import com.zenaton.engine.topics.StaterInterface
import com.zenaton.engine.topics.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptDispatched
import com.zenaton.engine.topics.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.topics.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.topics.tasks.messages.TaskCompleted
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.engine.topics.tasks.messages.TaskMessageInterface

class TaskEngine(
    private val stater: StaterInterface<TaskState>,
    private val dispatcher: TaskDispatcherInterface,
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
            state = TaskState(taskId = msg.taskId)
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
            is TaskAttemptDispatched -> dispatchTaskAttempt(state, msg)
            is TaskAttemptFailed -> failTaskAttempt(state, msg)
            is TaskAttemptStarted -> startTaskAttempt(state, msg)
            is TaskCompleted -> completeTask(state, msg)
            is TaskDispatched -> dispatchTask(state, msg)
        }
    }

    private fun completeTaskAttempt(state: TaskState, msg: TaskAttemptCompleted) {
        TODO()
    }

    private fun dispatchTaskAttempt(state: TaskState, msg: TaskAttemptDispatched) {
        TODO()
    }

    private fun failTaskAttempt(state: TaskState, msg: TaskAttemptFailed) {
        TODO()
    }

    private fun startTaskAttempt(state: TaskState, msg: TaskAttemptStarted) {
        TODO()
    }

    private fun completeTask(state: TaskState, msg: TaskCompleted) {
        TODO()
    }

    private fun dispatchTask(state: TaskState, msg: TaskDispatched) {
        TODO()
    }
}
