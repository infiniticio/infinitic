package com.zenaton.taskManager.monitoring.global

interface MonitoringGlobalStorage {
    fun getState(): MonitoringGlobalState?

    fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?)

    fun deleteState()
}
