package com.zenaton.taskManager.engine

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.JobAttemptError
import com.zenaton.taskManager.data.JobAttemptId
import com.zenaton.taskManager.data.JobData
import com.zenaton.taskManager.data.JobId
import com.zenaton.taskManager.data.JobName
import com.zenaton.taskManager.data.JobOutput
import com.zenaton.taskManager.messages.FailingJobAttemptMessage
import com.zenaton.taskManager.messages.JobAttemptMessage
import com.zenaton.taskManager.messages.JobMessage
import com.zenaton.workflowengine.data.WorkflowId

sealed class EngineMessage(
    override val jobId: JobId,
    override val sentAt: DateTime
) : JobMessage

data class CancelJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(jobId, sentAt)

data class DispatchJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName,
    val jobData: JobData?,
    val workflowId: WorkflowId? = null
) : EngineMessage(jobId, sentAt)

data class RetryJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(jobId, sentAt)

data class RetryJobAttempt(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int
) : EngineMessage(jobId, sentAt), JobAttemptMessage

data class JobAttemptCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    val jobOutput: JobOutput?
) : EngineMessage(jobId, sentAt), JobAttemptMessage

data class JobAttemptDispatched(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(jobId, sentAt), JobAttemptMessage

data class JobAttemptFailed(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    override val jobAttemptDelayBeforeRetry: Float?,
    val jobAttemptError: JobAttemptError
) : EngineMessage(jobId, sentAt), FailingJobAttemptMessage

data class JobAttemptStarted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int
) : EngineMessage(jobId, sentAt), JobAttemptMessage

data class JobCanceled(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(jobId, sentAt), JobMessage

data class JobCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobOutput: JobOutput?
) : EngineMessage(jobId, sentAt), JobMessage

data class JobDispatched(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(jobId, sentAt), JobMessage
