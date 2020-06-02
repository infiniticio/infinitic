package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage

data class RetryJobAttempt(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int
) : JobAttemptMessage, EngineMessage, MonitoringPerInstanceMessage
