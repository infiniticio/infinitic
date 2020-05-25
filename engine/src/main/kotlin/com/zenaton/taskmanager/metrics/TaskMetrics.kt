package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.state.StateStorage

class TaskMetrics(private val stateStorage: StateStorage) {
    fun handle(message: TaskMetricMessage) {
        if (message is TaskStatusUpdated) {
            decrementOldCounter(message)
            incrementNewCounter(message)
        }
    }

    private fun decrementOldCounter(message: TaskStatusUpdated) {
        message.oldStatus?.let {
            val counterKey = "metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
            stateStorage.incrCounter(counterKey, -1)
        }
    }

    private fun incrementNewCounter(message: TaskStatusUpdated) {
        message.newStatus?.let {
            val counterKey = "metrics.rt.counter.task.${message.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
            stateStorage.incrCounter(counterKey, 1)
        }
    }
}
