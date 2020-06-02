package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage

data class CancelJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : ForMonitoringPerInstanceMessage, ForEngineMessage
