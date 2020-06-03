package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptError
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.messages.interfaces.FailingJobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.JobMessage
import com.zenaton.workflowengine.data.WorkflowId

sealed class Message

data class CancelJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : Message(), ForMonitoringPerInstanceMessage, ForEngineMessage

data class DispatchJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName,
    val jobData: JobData?,
    val workflowId: WorkflowId? = null
) : Message(), ForEngineMessage, ForMonitoringPerInstanceMessage

data class JobAttemptCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    val jobOutput: JobOutput?
) : Message(), JobAttemptMessage, ForEngineMessage, ForMonitoringPerInstanceMessage

data class JobAttemptDispatched(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : Message(), JobAttemptMessage, ForMonitoringPerInstanceMessage

data class JobAttemptFailed(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    override val jobAttemptDelayBeforeRetry: Float?,
    val jobAttemptError: JobAttemptError
) : Message(), FailingJobAttemptMessage, ForEngineMessage, ForMonitoringPerInstanceMessage

data class JobAttemptStarted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int
) : Message(), JobAttemptMessage, ForEngineMessage, ForMonitoringPerInstanceMessage

data class JobCanceled(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : Message(), JobMessage, ForMonitoringPerInstanceMessage

data class JobCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobOutput: JobOutput?
) : Message(), JobMessage, ForMonitoringPerInstanceMessage

data class JobCreated(
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName
) : Message(), ForMonitoringGlobalMessage

data class JobDispatched(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : Message(), JobMessage, ForMonitoringPerInstanceMessage

data class JobStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val jobName: JobName,
    val jobId: JobId,
    val oldStatus: JobStatus?,
    val newStatus: JobStatus
) : Message(), ForMonitoringPerNameMessage

data class RetryJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : Message(), ForEngineMessage, ForMonitoringPerInstanceMessage

data class RetryJobAttempt(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int
) : Message(), JobAttemptMessage, ForEngineMessage, ForMonitoringPerInstanceMessage

data class RunJob(
    override val jobName: JobName,
    override val sentAt: DateTime = DateTime(),
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    val jobData: JobData?
) : Message(), JobAttemptMessage, ForWorkerMessage
