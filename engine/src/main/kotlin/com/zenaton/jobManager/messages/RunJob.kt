package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage

data class RunJob(
    override val jobName: JobName,
    override val sentAt: DateTime = DateTime(),
    override val jobId: JobId,
    override val jobAttemptId: JobAttemptId,
    override val jobAttemptRetry: Int,
    override val jobAttemptIndex: Int,
    val jobData: JobData?
) : JobAttemptMessage, ForWorkerMessage
