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

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.storage.api.Storage
import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.states.TaskEngineState
import io.infinitic.avro.taskManager.data.states.AvroMonitoringGlobalState
import io.infinitic.avro.taskManager.data.states.AvroMonitoringPerNameState
import io.infinitic.avro.taskManager.data.states.AvroTaskEngineState

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class AvroKeyValueTaskStateStorage(protected val storage: Storage) : TaskStateStorage {
    override fun getMonitoringGlobalState(): MonitoringGlobalState? {
        return storage
            .getState(getMonitoringGlobalStateKey())
            ?.let { AvroSerDe.deserialize<AvroMonitoringGlobalState>(it) }
            ?.let { AvroConverter.fromStorage(it) }
    }

    override fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        AvroConverter.toStorage(newState)
            .let { AvroSerDe.serialize(it) }
            .let { storage.putState(getMonitoringGlobalStateKey(), it) }
    }

    override fun deleteMonitoringGlobalState() {
        storage.deleteState(getMonitoringGlobalStateKey())
    }

    override fun getMonitoringPerNameState(taskName: TaskName): MonitoringPerNameState? {
        return storage.getState(getMonitoringPerNameStateKey(taskName))
            ?.let { AvroSerDe.deserialize<AvroMonitoringPerNameState>(it) }
            ?.let { AvroConverter.fromStorage(it) }
    }

    override fun updateMonitoringPerNameState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        val counterOkKey = getMonitoringPerNameCounterKey(taskName, TaskStatus.RUNNING_OK)
        val counterWarningKey = getMonitoringPerNameCounterKey(taskName, TaskStatus.RUNNING_WARNING)
        val counterErrorKey = getMonitoringPerNameCounterKey(taskName, TaskStatus.RUNNING_ERROR)
        val counterCompletedKey = getMonitoringPerNameCounterKey(taskName, TaskStatus.TERMINATED_COMPLETED)
        val counterCanceledKey = getMonitoringPerNameCounterKey(taskName, TaskStatus.TERMINATED_CANCELED)

        // use counters to save state, to avoid race conditions
        val incrOk = newState.runningOkCount - (oldState?.runningOkCount ?: 0L)
        val incrWarning = newState.runningWarningCount - (oldState?.runningWarningCount ?: 0L)
        val incrError = newState.runningErrorCount - (oldState?.runningErrorCount ?: 0L)
        val incrCompleted = newState.terminatedCompletedCount - (oldState?.terminatedCompletedCount ?: 0L)
        val incrCanceled = newState.terminatedCanceledCount - (oldState?.terminatedCanceledCount ?: 0L)

        incrementCounter(counterOkKey, incrOk, force = oldState == null)
        incrementCounter(counterWarningKey, incrWarning, force = oldState == null)
        incrementCounter(counterErrorKey, incrError, force = oldState == null)
        incrementCounter(counterCompletedKey, incrCompleted, force = oldState == null)
        incrementCounter(counterCanceledKey, incrCanceled, force = oldState == null)

        // save state retrieved from counters
        val state = AvroMonitoringPerNameState.newBuilder().apply {
            setTaskName("$taskName")
            runningOkCount = storage.getCounter(counterOkKey)
            runningWarningCount = storage.getCounter(counterWarningKey)
            runningErrorCount = storage.getCounter(counterErrorKey)
            terminatedCompletedCount = storage.getCounter(counterCompletedKey)
            terminatedCanceledCount = storage.getCounter(counterCanceledKey)
        }.build()

        storage.putState(getMonitoringPerNameStateKey(taskName), AvroSerDe.serialize(state))
    }

    override fun deleteMonitoringPerNameState(taskName: TaskName) {
        storage.deleteState(getMonitoringPerNameStateKey(taskName))
    }

    override fun getTaskEngineState(taskId: TaskId): TaskEngineState? {
        return storage.getState(getEngineStateKey("$taskId"))
            ?.let { AvroSerDe.deserialize<AvroTaskEngineState>(it) }
            ?.let { AvroConverter.fromStorage(it) }
    }

    override fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        AvroConverter.toStorage(newState)
            .let { AvroSerDe.serialize(it) }
            .let { storage.putState(getEngineStateKey("$taskId"), it) }
    }

    override fun deleteTaskEngineState(taskId: TaskId) {
        storage.deleteState(getEngineStateKey("$taskId"))
    }

    private fun incrementCounter(key: String, amount: Long, force: Boolean = false) {
        if (force || amount != 0L) {
            storage.incrementCounter(key, amount)
        }
    }

    internal fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"
    internal fun getMonitoringPerNameStateKey(taskName: TaskName) = "monitoringPerName.state.$taskName"
    internal fun getMonitoringPerNameCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "monitoringPerName.counter.${taskStatus.toString().toLowerCase()}.${taskName.toString().toLowerCase()}"
    internal fun getEngineStateKey(taskId: String) = "engine.state.task.$taskId"
}
