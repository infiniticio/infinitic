package com.zenaton.taskManager.workers

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.JobAttemptId
import com.zenaton.taskManager.data.JobData
import com.zenaton.taskManager.data.JobId
import com.zenaton.taskManager.data.JobName
import com.zenaton.taskManager.messages.JobAttemptMessage

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
