package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.jobManager.messages.interfaces.JobMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage

data class JobCompleted(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobOutput: JobOutput?
) : JobMessage, ForMonitoringPerInstanceMessage
