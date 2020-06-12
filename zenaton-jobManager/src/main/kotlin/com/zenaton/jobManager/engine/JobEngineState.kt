package com.zenaton.jobManager.engine

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.data.WorkflowId

data class JobEngineState(
    val jobId: JobId,
    val jobName: JobName,
    val jobStatus: JobStatus,
    val jobInput: JobInput,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: JobAttemptIndex = JobAttemptIndex(0),
    var jobAttemptRetry: JobAttemptRetry = JobAttemptRetry(0),
    val workflowId: WorkflowId? = null
) : StateInterface
