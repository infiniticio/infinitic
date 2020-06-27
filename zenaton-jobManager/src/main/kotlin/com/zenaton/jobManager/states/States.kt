package com.zenaton.jobManager.states

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobMeta
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus

sealed class State

data class JobEngineState(
    val jobId: JobId,
    val jobName: JobName,
    val jobStatus: JobStatus,
    val jobInput: JobInput,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: JobAttemptIndex = JobAttemptIndex(0),
    var jobAttemptRetry: JobAttemptRetry = JobAttemptRetry(0),
    val jobMeta: JobMeta
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}

data class MonitoringPerNameState(
    val jobName: JobName,
    var runningOkCount: Long = 0,
    var runningWarningCount: Long = 0,
    var runningErrorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}

data class MonitoringGlobalState(
    val jobNames: MutableSet<JobName> = mutableSetOf()
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}
