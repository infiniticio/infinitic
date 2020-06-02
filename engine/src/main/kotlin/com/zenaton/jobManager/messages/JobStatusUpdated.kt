package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage

data class JobStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val jobName: JobName,
    val jobId: JobId,
    val oldStatus: JobStatus?,
    val newStatus: JobStatus
) : ForMonitoringPerNameMessage
