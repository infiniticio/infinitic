package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptError
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.FailingJobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage

data class JobAttemptFailed(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    override val jobAttemptDelayBeforeRetry: Float?,
    val jobAttemptError: JobAttemptError
) : FailingJobAttemptMessage, EngineMessage, MonitoringPerInstanceMessage
