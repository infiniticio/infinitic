package com.zenaton.taskManager.common.messages

import com.zenaton.taskManager.common.data.JobAttemptError
import com.zenaton.taskManager.common.data.JobAttemptId
import com.zenaton.taskManager.common.data.JobAttemptIndex
import com.zenaton.taskManager.common.data.JobAttemptRetry
import com.zenaton.taskManager.common.data.JobId
import com.zenaton.taskManager.common.data.JobInput
import com.zenaton.taskManager.common.data.JobMeta
import com.zenaton.taskManager.common.data.JobName
import com.zenaton.taskManager.common.data.JobOptions
import com.zenaton.taskManager.common.data.JobOutput
import com.zenaton.taskManager.common.data.JobStatus
import com.zenaton.taskManager.common.messages.interfaces.FailingJobAttemptMessage
import com.zenaton.taskManager.common.messages.interfaces.JobAttemptMessage

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
    val jobMeta: JobMeta,
    val jobOptions: JobOptions
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
    val jobOutput: JobOutput,
    val jobMeta: JobMeta
) : ForJobEngineMessage(jobId)

data class JobCompleted(
    override val jobId: JobId,
    val jobOutput: JobOutput,
    val jobMeta: JobMeta
) : ForJobEngineMessage(jobId)

data class RetryJob(
    override val jobId: JobId,
    val jobName: JobName?,
    val jobInput: JobInput?,
    val jobMeta: JobMeta?,
    val jobOptions: JobOptions?
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
    val jobInput: JobInput,
    val jobOptions: JobOptions,
    val jobMeta: JobMeta
) : ForWorkerMessage(jobName), JobAttemptMessage
