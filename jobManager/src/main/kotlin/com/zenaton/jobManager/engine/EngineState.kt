package com.zenaton.jobManager.engine

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.workflowengine.data.WorkflowId

data class EngineState(
    val jobId: JobId,
    val jobName: JobName,
    var jobStatus: JobStatus,
    val jobData: JobData?,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: JobAttemptIndex = JobAttemptIndex(0),
    var jobAttemptRetry: JobAttemptRetry = JobAttemptRetry(0),
    val workflowId: WorkflowId? = null
) : StateInterface
