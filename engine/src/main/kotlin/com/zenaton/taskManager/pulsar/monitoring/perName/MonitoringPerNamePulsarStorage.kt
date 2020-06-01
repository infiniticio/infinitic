package com.zenaton.taskManager.pulsar.monitoring.perName

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameStorage
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.taskManager.states.AvroTaskMetricsState
import org.apache.pulsar.functions.api.Context

class MonitoringPerNamePulsarStorage(val context: Context) : MonitoringPerNameStorage {
    var avroSerDe = AvroSerDe
    var avroConverter = AvroConverter
    var taskDispatcher = PulsarDispatcher(context)

    override fun getState(taskName: TaskName): MonitoringPerNameState? =
        context.getState(getStateKey(taskName))?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskMetricsState>(it)) }

    override fun updateState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        val counterOkKey = getCounterKey(taskName, TaskStatus.RUNNING_OK)
        val counterWarningKey = getCounterKey(taskName, TaskStatus.RUNNING_WARNING)
        val counterErrorKey = getCounterKey(taskName, TaskStatus.RUNNING_ERROR)
        val counterCompletedKey = getCounterKey(taskName, TaskStatus.TERMINATED_COMPLETED)
        val counterCanceledKey = getCounterKey(taskName, TaskStatus.TERMINATED_CANCELED)

        // use counters to save state, to avoid race conditions
        val incrOk = newState.runningOkCount - (oldState?.runningOkCount ?: 0L)
        val incrWarning = newState.runningWarningCount - (oldState?.runningWarningCount ?: 0L)
        val incrError = newState.runningErrorCount - (oldState?.runningErrorCount ?: 0L)
        val incrCompleted = newState.terminatedCompletedCount - (oldState?.terminatedCompletedCount ?: 0L)
        val incrCanceled = newState.terminatedCanceledCount - (oldState?.terminatedCanceledCount ?: 0L)

        if (incrOk != 0L) incrCounter(counterOkKey, incrOk)
        if (incrWarning != 0L) incrCounter(counterWarningKey, incrWarning)
        if (incrError != 0L) incrCounter(counterErrorKey, incrError)
        if (incrCompleted != 0L) incrCounter(counterCompletedKey, incrCompleted)
        if (incrCanceled != 0L) incrCounter(counterCanceledKey, incrCanceled)

        // save state retrieved from counters
        val state = MonitoringPerNameState(
            taskName = taskName,
            runningOkCount = getCounter(counterOkKey),
            runningWarningCount = getCounter(counterWarningKey),
            runningErrorCount = getCounter(counterErrorKey),
            terminatedCompletedCount = getCounter(counterCompletedKey),
            terminatedCanceledCount = getCounter(counterCanceledKey)
        )
        context.putState(getStateKey(taskName), avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun deleteState(taskName: TaskName) {
        context.deleteState(getStateKey(taskName))
    }

    fun getStateKey(taskName: TaskName) = "metrics.task.${taskName.name.toLowerCase()}.counters"

    fun getCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "metrics.rt.counter.task.${taskName.name.toLowerCase()}.${taskStatus.toString().toLowerCase()}"

    private fun incrCounter(key: String, amount: Long) = context.incrCounter(key, amount)

    private fun getCounter(key: String) = context.getCounter(key)
}
