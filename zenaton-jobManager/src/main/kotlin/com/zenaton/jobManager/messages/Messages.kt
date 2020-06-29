package com.zenaton.jobManager.messages

import com.zenaton.jobManager.data.JobAttemptError
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobMeta
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.messages.interfaces.FailingJobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage

sealed class Message

sealed class ForJobEngineMessage(open val jobId: JobId) : Message()

sealed class ForMonitoringPerNameMessage(open val jobName: JobName) : Message()

sealed class ForMonitoringGlobalMessage : Message()

sealed class ForWorkerMessage(open val jobName: JobName) : Message()

/*
 * Job Engine Messages
 */

data class CancelJob(
    override val jobId: JobId,
    val jobOutput: JobOutput
) : ForJobEngineMessage(jobId)

data class DispatchJob(
    override val jobId: JobId,
    val jobName: JobName,
    val jobInput: JobInput,
    val jobMeta: JobMeta
) : ForJobEngineMessage(jobId)

data class JobAttemptCompleted(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex,
    val jobOutput: JobOutput
) : ForJobEngineMessage(jobId), JobAttemptMessage

data class JobAttemptDispatched(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex
) : ForJobEngineMessage(jobId), JobAttemptMessage

data class JobAttemptFailed(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex,
    override val jobAttemptDelayBeforeRetry: Float?,
    val jobAttemptError: JobAttemptError
) : ForJobEngineMessage(jobId), FailingJobAttemptMessage

data class JobAttemptStarted(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex
) : ForJobEngineMessage(jobId), JobAttemptMessage

data class JobCanceled(
    override val jobId: JobId,
    val jobMeta: JobMeta
) : ForJobEngineMessage(jobId)

data class JobCompleted(
    override val jobId: JobId,
    val jobOutput: JobOutput,
    val jobMeta: JobMeta
) : ForJobEngineMessage(jobId)

data class RetryJob(
    override val jobId: JobId
) : ForJobEngineMessage(jobId)

data class RetryJobAttempt(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex
) : ForJobEngineMessage(jobId), JobAttemptMessage

/*
 * Monitoring Per Name Messages
 */

data class JobStatusUpdated constructor(
    override val jobName: JobName,
    val jobId: JobId,
    val oldStatus: JobStatus?,
    val newStatus: JobStatus
) : ForMonitoringPerNameMessage(jobName)

/*
 * Monitoring Global Messages
 */

data class JobCreated(
    val jobName: JobName
) : ForMonitoringGlobalMessage()

/*
 * Worker Messages
 */

data class RunJob(
    override val jobName: JobName,
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: JobAttemptRetry,
    override val jobAttemptIndex: JobAttemptIndex,
    val jobInput: JobInput
) : ForWorkerMessage(jobName), JobAttemptMessage
