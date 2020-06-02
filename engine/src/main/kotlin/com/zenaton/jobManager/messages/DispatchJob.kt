package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage
import com.zenaton.workflowengine.data.WorkflowId

data class DispatchJob(
    override val jobId: JobId,
    override val sentAt: DateTime = DateTime(),
    val jobName: JobName,
    val jobData: JobData?,
    val workflowId: WorkflowId? = null
) : EngineMessage, MonitoringPerInstanceMessage
