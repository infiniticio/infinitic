package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage

class TaskMetrics {
    lateinit var stateStorage: TaskMetricsStateStorage

    fun handle(message: TaskMetricMessage) {
        if (message is TaskStatusUpdated) {
            stateStorage.updateTaskStatusCountersByName(message.taskName, message.oldStatus, message.newStatus)
        }
    }
}
