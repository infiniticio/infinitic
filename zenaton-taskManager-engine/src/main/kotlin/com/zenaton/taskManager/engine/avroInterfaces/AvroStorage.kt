package com.zenaton.taskManager.engine.avroInterfaces

import com.zenaton.taskManager.states.AvroTaskEngineState
import com.zenaton.taskManager.states.AvroMonitoringGlobalState
import com.zenaton.taskManager.states.AvroMonitoringPerNameState

interface AvroStorage {
    fun getTaskEngineState(taskId: String): AvroTaskEngineState?
    fun updateTaskEngineState(taskId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?)
    fun deleteTaskEngineState(taskId: String)

    fun getMonitoringPerNameState(taskName: String): AvroMonitoringPerNameState?
    fun updateMonitoringPerNameState(taskName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?)
    fun deleteMonitoringPerNameState(taskName: String)

    fun getMonitoringGlobalState(): AvroMonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?)
    fun deleteMonitoringGlobalState()
}
