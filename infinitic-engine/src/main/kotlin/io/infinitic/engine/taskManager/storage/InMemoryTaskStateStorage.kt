// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

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
