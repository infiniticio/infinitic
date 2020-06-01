package com.zenaton.taskManager.engine

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskManager.data.JobAttemptId
import com.zenaton.taskManager.data.JobData
import com.zenaton.taskManager.data.JobId
import com.zenaton.taskManager.data.JobName
import com.zenaton.taskManager.data.JobStatus
import com.zenaton.workflowengine.data.WorkflowId

data class EngineState(
    val jobId: JobId,
    val jobName: JobName,
    var jobStatus: JobStatus,
    val jobData: JobData?,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: Int,
    val workflowId: WorkflowId? = null
) : StateInterface
