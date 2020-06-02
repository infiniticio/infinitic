package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage

data class JobAttemptDispatched(
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : JobAttemptMessage, ForMonitoringPerInstanceMessage
