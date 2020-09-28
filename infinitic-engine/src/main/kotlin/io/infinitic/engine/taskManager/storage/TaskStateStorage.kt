package io.infinitic.engine.taskManager.storage

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.states.TaskEngineState

/**
 * TaskStateStorage implementations are responsible for storing the different state objects used by the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data should be
 * transformed before being stored. These details are left to the different implementations.
 */
interface TaskStateStorage {
    fun getMonitoringGlobalState(): MonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?)
    fun deleteMonitoringGlobalState()

    fun getMonitoringPerNameState(taskName: TaskName): MonitoringPerNameState?
    fun updateMonitoringPerNameState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?)
    fun deleteMonitoringPerNameState(taskName: TaskName)

    fun getTaskEngineState(taskId: TaskId): TaskEngineState?
    fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?)
    fun deleteTaskEngineState(taskId: TaskId)
}
