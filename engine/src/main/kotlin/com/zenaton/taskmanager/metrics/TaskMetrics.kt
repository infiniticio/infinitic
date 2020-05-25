package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.state.StateStorage

class TaskMetrics() {
    lateinit var stateStorage: StateStorage

    fun handle(message: TaskMetricMessage) {
        if (message is TaskStatusUpdated) {
            decrementOldCounter(message)
            incrementNewCounter(message)
        }
    }

    private fun decrementOldCounter(message: TaskStatusUpdated) =
        message.oldStatus?.let {
            stateStorage.incrCounter(getCounterKey(message.taskName, it), -1)
        }

    private fun incrementNewCounter(message: TaskStatusUpdated) =
        message.newStatus?.let {
            stateStorage.incrCounter(getCounterKey(message.taskName, it), 1)
        }

    private fun getCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "metrics.rt.counter.task.${taskName.name.toLowerCase()}.${taskStatus.toString().toLowerCase()}"
}
