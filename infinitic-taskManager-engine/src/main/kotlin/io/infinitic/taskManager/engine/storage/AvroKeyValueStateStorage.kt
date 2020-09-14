package io.infinitic.taskManager.engine.storage

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.storage.api.Storage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.common.states.MonitoringGlobalState
import io.infinitic.taskManager.common.states.MonitoringPerNameState
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import io.infinitic.taskManager.states.AvroTaskEngineState

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class AvroKeyValueStateStorage(private val storage: Storage) : StateStorage {
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
            setTaskName(taskName.name)
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
        return storage.getState(getEngineStateKey(taskId.id))
            ?.let { AvroSerDe.deserialize<AvroTaskEngineState>(it) }
            ?.let { AvroConverter.fromStorage(it) }
    }

    override fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        AvroConverter.toStorage(newState)
            .let { AvroSerDe.serialize(it) }
            .let { storage.putState(getEngineStateKey(taskId.id), it) }
    }

    override fun deleteTaskEngineState(taskId: TaskId) {
        storage.deleteState(getEngineStateKey(taskId.id))
    }

    private fun incrementCounter(key: String, amount: Long, force: Boolean = false) {
        if (force || amount != 0L) {
            storage.incrementCounter(key, amount)
        }
    }

    private fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"
    internal fun getMonitoringPerNameStateKey(taskName: TaskName) = "monitoringPerName.state.${taskName.name}"
    internal fun getMonitoringPerNameCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "monitoringPerName.counter.${taskStatus.toString().toLowerCase()}.${taskName.name.toLowerCase()}"
    private fun getEngineStateKey(taskId: String) = "engine.state.$taskId"
}
