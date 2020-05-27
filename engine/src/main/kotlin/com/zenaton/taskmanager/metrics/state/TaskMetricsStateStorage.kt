package com.zenaton.taskmanager.metrics.state

import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus

interface TaskMetricsStateStorage {
    fun updateTaskStatusCountersByName(taskName: TaskName, oldStatus: TaskStatus?, newStatus: TaskStatus)
}
