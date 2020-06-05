package com.zenaton.jobManager.monitoring.perName

import com.zenaton.jobManager.data.JobName

interface MonitoringPerNameStorage {
    fun getState(jobName: JobName): MonitoringPerNameState?

    fun updateState(jobName: JobName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?)

    fun deleteState(jobName: JobName)
}
