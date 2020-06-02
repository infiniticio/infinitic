package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage

data class JobCreated(
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName
) : ForMonitoringGlobalMessage
