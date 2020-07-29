package com.zenaton.jobManager.common.states

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.JobAttemptId
import com.zenaton.jobManager.common.data.JobAttemptIndex
import com.zenaton.jobManager.common.data.JobAttemptRetry
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.data.JobStatus

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
