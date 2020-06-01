package com.zenaton.jobManager.workers

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.JobAttemptMessage

sealed class WorkerMessage {
    abstract val jobName: JobName
    abstract val sentAt: DateTime
}

data class RunJob(
    override val jobName: JobName,
    override val sentAt: DateTime = DateTime(),
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptIndex: Int,
    val jobData: JobData?
) : WorkerMessage(), JobAttemptMessage
