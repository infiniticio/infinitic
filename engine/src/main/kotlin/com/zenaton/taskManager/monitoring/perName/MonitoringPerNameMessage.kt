package com.zenaton.taskManager.monitoring.perName

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.JobId
import com.zenaton.taskManager.data.JobName
import com.zenaton.taskManager.data.JobStatus

sealed class MonitoringPerNameMessage {
    abstract val sentAt: DateTime
    abstract val jobName: JobName
}

data class JobStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val jobName: JobName,
    val jobId: JobId,
    val oldStatus: JobStatus?,
    val newStatus: JobStatus
) : MonitoringPerNameMessage()
