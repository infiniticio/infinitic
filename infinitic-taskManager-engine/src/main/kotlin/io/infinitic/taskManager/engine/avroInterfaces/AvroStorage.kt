package io.infinitic.taskManager.engine.avroInterfaces

import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState

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
