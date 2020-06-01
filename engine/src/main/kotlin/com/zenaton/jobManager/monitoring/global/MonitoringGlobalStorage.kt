package com.zenaton.jobManager.monitoring.global

interface MonitoringGlobalStorage {
    fun getState(): MonitoringGlobalState?

    fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?)

    fun deleteState()
}
