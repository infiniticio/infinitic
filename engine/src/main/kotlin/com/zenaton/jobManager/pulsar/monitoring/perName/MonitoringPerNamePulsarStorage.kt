package com.zenaton.jobManager.pulsar.monitoring.perName

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameStorage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import org.apache.pulsar.functions.api.Context

class MonitoringPerNamePulsarStorage(val context: Context) : MonitoringPerNameStorage {
    var avroSerDe = AvroSerDe
    var avroConverter = AvroConverter
    var taskDispatcher = PulsarDispatcher(context)

    override fun getState(jobName: JobName): MonitoringPerNameState? =
        context.getState(getStateKey(jobName))?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroMonitoringPerNameState>(it)) }

    override fun updateState(jobName: JobName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        val counterOkKey = getCounterKey(jobName, JobStatus.RUNNING_OK)
        val counterWarningKey = getCounterKey(jobName, JobStatus.RUNNING_WARNING)
        val counterErrorKey = getCounterKey(jobName, JobStatus.RUNNING_ERROR)
        val counterCompletedKey = getCounterKey(jobName, JobStatus.TERMINATED_COMPLETED)
        val counterCanceledKey = getCounterKey(jobName, JobStatus.TERMINATED_CANCELED)

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
            jobName = jobName,
            runningOkCount = getCounter(counterOkKey),
            runningWarningCount = getCounter(counterWarningKey),
            runningErrorCount = getCounter(counterErrorKey),
            terminatedCompletedCount = getCounter(counterCompletedKey),
            terminatedCanceledCount = getCounter(counterCanceledKey)
        )
        context.putState(getStateKey(jobName), avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun deleteState(jobName: JobName) {
        context.deleteState(getStateKey(jobName))
    }

    fun getStateKey(jobName: JobName) = "metrics.task.${jobName.name.toLowerCase()}.counters"

    fun getCounterKey(jobName: JobName, jobStatus: JobStatus) = "metrics.rt.counter.task.${jobName.name.toLowerCase()}.${jobStatus.toString().toLowerCase()}"

    private fun incrCounter(key: String, amount: Long) = context.incrCounter(key, amount)

    private fun getCounter(key: String) = context.getCounter(key)
}
