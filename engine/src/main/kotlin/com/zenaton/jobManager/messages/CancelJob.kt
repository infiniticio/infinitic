package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage

data class CancelJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : MonitoringPerInstanceMessage, EngineMessage
