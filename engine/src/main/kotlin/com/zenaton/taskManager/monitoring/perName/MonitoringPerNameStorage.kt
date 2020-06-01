package com.zenaton.taskManager.monitoring.perName

import com.zenaton.taskManager.data.JobName

interface MonitoringPerNameStorage {
    fun getState(jobName: JobName): MonitoringPerNameState?

    fun updateState(jobName: JobName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?)

    fun deleteState(jobName: JobName)
}
