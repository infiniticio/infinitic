package com.zenaton.jobManager.monitoring.global

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobName

sealed class MonitoringGlobalMessage {
    abstract val sentAt: DateTime
    abstract val jobName: JobName
}

data class JobCreated(
    override val sentAt: DateTime = DateTime(),
    override val jobName: JobName
) : MonitoringGlobalMessage()
