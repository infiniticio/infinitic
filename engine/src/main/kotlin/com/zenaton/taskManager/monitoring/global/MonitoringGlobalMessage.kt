package com.zenaton.taskManager.monitoring.global

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.JobName

sealed class MonitoringGlobalMessage {
    abstract val sentAt: DateTime
    abstract val jobName: JobName
}

data class JobCreated(
    override val sentAt: DateTime = DateTime(),
    override val jobName: JobName
) : MonitoringGlobalMessage()
