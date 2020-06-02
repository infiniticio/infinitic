package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.messages.interfaces.JobMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage

data class JobDispatched(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime()
) : JobMessage, MonitoringPerInstanceMessage
