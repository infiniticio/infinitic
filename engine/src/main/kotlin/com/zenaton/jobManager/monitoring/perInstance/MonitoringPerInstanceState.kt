package com.zenaton.jobManager.monitoring.perInstance

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.workflowengine.data.WorkflowId

data class MonitoringPerInstanceState(
    val jobId: JobId,
    val jobName: JobName,
    var jobStatus: JobStatus,
    val jobData: JobData?,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: Int,
    var jobAttemptRetry: Int,
    val workflowId: WorkflowId? = null
) : StateInterface
