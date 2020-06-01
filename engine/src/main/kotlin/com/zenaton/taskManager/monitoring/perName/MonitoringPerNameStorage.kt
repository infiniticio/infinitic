package com.zenaton.taskManager.monitoring.perName

import com.zenaton.taskManager.data.TaskName

interface MonitoringPerNameStorage {
    fun getState(taskName: TaskName): MonitoringPerNameState?

    fun updateState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?)

    fun deleteState(taskName: TaskName)
}
