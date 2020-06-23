package com.zenaton.jobManager.engine

import com.zenaton.common.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobMeta
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus

data class JobEngineState(
    val jobId: JobId,
    val jobName: JobName,
    val jobStatus: JobStatus,
    val jobInput: JobInput,
    var jobAttemptId: JobAttemptId,
    var jobAttemptIndex: JobAttemptIndex = JobAttemptIndex(0),
    var jobAttemptRetry: JobAttemptRetry = JobAttemptRetry(0),
    val jobMeta: JobMeta
) : StateInterface
