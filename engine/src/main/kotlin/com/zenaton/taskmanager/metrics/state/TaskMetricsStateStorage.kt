package com.zenaton.taskmanager.metrics.state

import com.zenaton.taskmanager.data.TaskName

interface TaskMetricsStateStorage {
    fun getState(taskName: TaskName): TaskMetricsState?

    fun updateState(taskName: TaskName, newState: TaskMetricsState, oldState: TaskMetricsState?)

    fun deleteState(taskName: TaskName)
}
