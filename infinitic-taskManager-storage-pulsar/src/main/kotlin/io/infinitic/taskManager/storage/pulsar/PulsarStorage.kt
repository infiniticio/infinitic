package io.infinitic.taskManager.storage.pulsar

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import org.apache.pulsar.functions.api.Context

class PulsarStorage(val context: Context) : AvroStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    override fun getTaskEngineState(taskId: String): AvroTaskEngineState? {
        return context.getState(getEngineStateKey(taskId))?.let { avroSerDe.deserialize<AvroTaskEngineState>(it) }
    }

    override fun updateTaskEngineState(taskId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?) {
        context.putState(getEngineStateKey(taskId), avroSerDe.serialize(newState))
    }

    override fun deleteTaskEngineState(taskId: String) {
        context.deleteState(getEngineStateKey(taskId))
    }

    override fun getMonitoringPerNameState(taskName: String): AvroMonitoringPerNameState? =
        context.getState(getMonitoringPerNameStateKey(taskName))?.let { avroSerDe.deserialize<AvroMonitoringPerNameState>(it) }

    override fun updateMonitoringPerNameState(taskName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
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
            setTaskName(taskName)
            runningOkCount = context.getCounter(counterOkKey)
            runningWarningCount = context.getCounter(counterWarningKey)
            runningErrorCount = context.getCounter(counterErrorKey)
            terminatedCompletedCount = context.getCounter(counterCompletedKey)
            terminatedCanceledCount = context.getCounter(counterCanceledKey)
        }.build()

        context.putState(getMonitoringPerNameStateKey(taskName), avroSerDe.serialize(state))
    }

    private fun incrementCounter(key: String, amount: Long, force: Boolean = false) {
        if (force || amount != 0L) {
            context.incrCounter(key, amount)
        }
    }

    override fun deleteMonitoringPerNameState(taskName: String) {
        context.deleteState(getMonitoringPerNameStateKey(taskName))
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? {
        return context.getState(getMonitoringGlobalStateKey())?.let { avroSerDe.deserialize<AvroMonitoringGlobalState>(it) }
    }

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        context.putState(getMonitoringGlobalStateKey(), avroSerDe.serialize(newState))
    }

    override fun deleteMonitoringGlobalState() {
        context.deleteState(getMonitoringGlobalStateKey())
    }

    private fun getEngineStateKey(taskId: String) = "engine.state.$taskId"
    private fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"
    internal fun getMonitoringPerNameStateKey(taskName: String) = "monitoringPerName.state.$taskName"
    internal fun getMonitoringPerNameCounterKey(taskName: String, taskStatus: TaskStatus) = "monitoringPerName.counter.${taskStatus.toString().toLowerCase()}.${taskName.toLowerCase()}"
}
