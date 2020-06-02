package com.zenaton.jobManager.monitoring.perInstance

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptError
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.jobManager.messages.interfaces.FailingJobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.JobMessage
import com.zenaton.workflowengine.data.WorkflowId

sealed class MonitoringPerInstanceMessage(
    override val jobId: JobId,
    override val sentAt: DateTime
) : JobMessage

data class CancelJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage(jobId, sentAt)

data class DispatchJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName,
    val jobData: JobData?,
    val workflowId: WorkflowId? = null
) : MonitoringPerInstanceMessage(jobId, sentAt)

data class RetryJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage(jobId, sentAt)

data class RetryJobAttempt(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int
) : MonitoringPerInstanceMessage(jobId, sentAt), JobAttemptMessage

data class JobAttemptCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    val jobOutput: JobOutput?
) : MonitoringPerInstanceMessage(jobId, sentAt), JobAttemptMessage


data class JobAttemptFailed(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    override val jobAttemptDelayBeforeRetry: Float?,
    val jobAttemptError: JobAttemptError
) : MonitoringPerInstanceMessage(jobId, sentAt), FailingJobAttemptMessage

data class JobAttemptStarted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int
) : MonitoringPerInstanceMessage(jobId, sentAt), JobAttemptMessage

data class JobAttemptDispatched(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage(jobId, sentAt), JobAttemptMessage

data class JobCanceled(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage(jobId, sentAt), JobMessage

data class JobCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobOutput: JobOutput?
) : MonitoringPerInstanceMessage(jobId, sentAt), JobMessage

data class JobDispatched(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage(jobId, sentAt), JobMessage
