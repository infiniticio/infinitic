package com.zenaton.jobManager.monitoring.perName

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus

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
