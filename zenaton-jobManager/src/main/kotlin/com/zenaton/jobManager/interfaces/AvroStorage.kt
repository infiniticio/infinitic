package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState

interface AvroStorage {
    fun getEngineState(jobId: String): AvroEngineState?
    fun updateEngineState(jobId: String, newState: AvroEngineState, oldState: AvroEngineState?)
    fun deleteEngineState(jobId: String)

    fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState?
    fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?)
    fun deleteMonitoringPerNameState(jobName: String)

    fun getMonitoringGlobalState(): AvroMonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?)
    fun deleteMonitoringGlobalState()
}
