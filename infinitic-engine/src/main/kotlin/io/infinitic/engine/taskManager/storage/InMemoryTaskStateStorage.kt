package io.infinitic.engine.taskManager.storage

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.states.TaskEngineState

/**
 * InMemoryStateStorage stores various objects in memory.
 *
 * Objects are stored without any modifications or transformations applied, making this state storage
 * useful when used during development, to avoid using a database, or to inspect the content of the state
 * easily.
 *
 * This state storage is not suitable for production because it is not persistent.
 */
open class InMemoryTaskStateStorage : TaskStateStorage {
    var taskEngineStore: MutableMap<String, TaskEngineState> = mutableMapOf()
    var monitoringPerNameStore: MutableMap<String, MonitoringPerNameState> = mutableMapOf()
    var monitoringGlobalStore: MonitoringGlobalState? = null

    override fun getTaskEngineState(taskId: TaskId) = taskEngineStore["$taskId"]

    override fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        taskEngineStore["$taskId"] = newState
    }

    override fun deleteTaskEngineState(taskId: TaskId) {
        taskEngineStore.remove("$taskId")
    }

    override fun getMonitoringPerNameState(taskName: TaskName): MonitoringPerNameState? {
        return monitoringPerNameStore["$taskName"]
    }

    override fun updateMonitoringPerNameState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        monitoringPerNameStore["$taskName"] = newState
    }

    override fun deleteMonitoringPerNameState(taskName: TaskName) {
        monitoringPerNameStore.remove("$taskName")
    }

    override fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        monitoringGlobalStore = newState
    }
    override fun getMonitoringGlobalState() = monitoringGlobalStore

    override fun deleteMonitoringGlobalState() {
        monitoringGlobalStore = null
    }

    fun reset() {
        taskEngineStore.clear()
        monitoringPerNameStore.clear()
        monitoringGlobalStore = null
    }
}
