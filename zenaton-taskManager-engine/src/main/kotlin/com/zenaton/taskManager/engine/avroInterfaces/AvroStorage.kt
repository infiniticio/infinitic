package com.zenaton.taskManager.engine.avroInterfaces

import com.zenaton.taskManager.states.AvroTaskEngineState
import com.zenaton.taskManager.states.AvroMonitoringGlobalState
import com.zenaton.taskManager.states.AvroMonitoringPerNameState

interface AvroStorage {
    fun getTaskEngineState(jobId: String): AvroTaskEngineState?
    fun updateTaskEngineState(jobId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?)
    fun deleteTaskEngineState(jobId: String)

    fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState?
    fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?)
    fun deleteMonitoringPerNameState(jobName: String)

    fun getMonitoringGlobalState(): AvroMonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?)
    fun deleteMonitoringGlobalState()
}
