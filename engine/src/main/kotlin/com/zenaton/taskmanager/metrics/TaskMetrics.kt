package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.state.StateStorage

class TaskMetrics(private val stateStorage: StateStorage) {
    fun handle(msg: TaskMetricMessage) {
        if (msg is TaskStatusUpdated) {
            msg.oldStatus?.let {
                val counterKey = "metrics.rt.counter.task.${msg.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
                stateStorage.incrCounter(counterKey, -1)
            }
            msg.newStatus?.let {
                val counterKey = "metrics.rt.counter.task.${msg.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
                stateStorage.incrCounter(counterKey, 1)
            }
        }
    }
}
