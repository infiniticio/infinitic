package io.infinitic.taskManager.engine.storage

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.states.MonitoringGlobalState
import io.infinitic.taskManager.common.states.MonitoringPerNameState
import io.infinitic.taskManager.common.states.TaskEngineState

/**
 * InMemoryStateStorage stores various objects in memory.
 *
 * Objects are stored without any modifications or transformations applied, making this state storage
 * useful when used during development, to avoid using a database, or to inspect the content of the state
 * easily.
 *
 * This state storage is not suitable for production because it is not persistent.
 */
open class InMemoryStateStorage : StateStorage {
    protected var taskEngineStore: Map<String, TaskEngineState> = mapOf()
    protected var monitoringPerNameStore: Map<String, MonitoringPerNameState> = mapOf()
    protected var monitoringGlobalStore: MonitoringGlobalState? = null

    override fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        monitoringGlobalStore = newState
    }

    override fun getMonitoringPerNameState(taskName: TaskName): MonitoringPerNameState? {
        return monitoringPerNameStore[taskName.name]
    }

    override fun updateMonitoringPerNameState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        monitoringPerNameStore = monitoringPerNameStore.plus(taskName.name to newState)
    }

    override fun deleteMonitoringPerNameState(taskName: TaskName) {
        monitoringPerNameStore = monitoringPerNameStore.minus(taskName.name)
    }

    override fun getTaskEngineState(taskId: TaskId): TaskEngineState? {
        return taskEngineStore[taskId.id]
    }

    override fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        taskEngineStore = taskEngineStore.plus(taskId.id to newState)
    }

    override fun deleteTaskEngineState(taskId: TaskId) {
        taskEngineStore = taskEngineStore.minus(taskId.id)
    }

    override fun getMonitoringGlobalState(): MonitoringGlobalState? = monitoringGlobalStore

    override fun deleteMonitoringGlobalState() {
        monitoringGlobalStore = null
    }
}
