package com.zenaton.taskManager.pulsar.storage

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.taskManager.common.data.TaskStatus
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.states.AvroTaskEngineState
import com.zenaton.taskManager.states.AvroMonitoringGlobalState
import com.zenaton.taskManager.states.AvroMonitoringPerNameState
import org.apache.pulsar.functions.api.Context

class PulsarAvroStorage(val context: Context) : AvroStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    override fun getTaskEngineState(jobId: String): AvroTaskEngineState? {
        return context.getState(getEngineStateKey(jobId))?.let { avroSerDe.deserialize<AvroTaskEngineState>(it) }
    }

    override fun updateTaskEngineState(jobId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?) {
        context.putState(getEngineStateKey(jobId), avroSerDe.serialize(newState))
    }

    override fun deleteTaskEngineState(jobId: String) {
        context.deleteState(getEngineStateKey(jobId))
    }

    override fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState? =
        context.getState(getMonitoringPerNameStateKey(jobName))?.let { avroSerDe.deserialize<AvroMonitoringPerNameState>(it) }

    override fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        val counterOkKey = getMonitoringPerNameCounterKey(jobName, TaskStatus.RUNNING_OK)
        val counterWarningKey = getMonitoringPerNameCounterKey(jobName, TaskStatus.RUNNING_WARNING)
        val counterErrorKey = getMonitoringPerNameCounterKey(jobName, TaskStatus.RUNNING_ERROR)
        val counterCompletedKey = getMonitoringPerNameCounterKey(jobName, TaskStatus.TERMINATED_COMPLETED)
        val counterCanceledKey = getMonitoringPerNameCounterKey(jobName, TaskStatus.TERMINATED_CANCELED)

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
            setTaskName(jobName)
            runningOkCount = context.getCounter(counterOkKey)
            runningWarningCount = context.getCounter(counterWarningKey)
            runningErrorCount = context.getCounter(counterErrorKey)
            terminatedCompletedCount = context.getCounter(counterCompletedKey)
            terminatedCanceledCount = context.getCounter(counterCanceledKey)
        }.build()

        context.putState(getMonitoringPerNameStateKey(jobName), avroSerDe.serialize(state))
    }

    private fun incrementCounter(key: String, amount: Long, force: Boolean = false) {
        if (force || amount != 0L) {
            context.incrCounter(key, amount)
        }
    }

    override fun deleteMonitoringPerNameState(jobName: String) {
        context.deleteState(getMonitoringPerNameStateKey(jobName))
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

    fun getEngineStateKey(jobId: String) = "engine.state.$jobId"
    fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"
    fun getMonitoringPerNameStateKey(jobName: String) = "monitoringPerName.state.$jobName"
    fun getMonitoringPerNameCounterKey(jobName: String, taskStatus: TaskStatus) = "monitoringPerName.counter.${taskStatus.toString().toLowerCase()}.${jobName.toLowerCase()}"
}
