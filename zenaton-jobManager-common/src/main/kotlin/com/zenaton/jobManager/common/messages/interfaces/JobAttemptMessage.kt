package com.zenaton.jobManager.common.messages.interfaces

import com.zenaton.jobManager.common.data.JobAttemptId
import com.zenaton.jobManager.common.data.JobAttemptIndex
import com.zenaton.jobManager.common.data.JobAttemptRetry
import com.zenaton.jobManager.common.data.JobId

interface JobAttemptMessage {
    val jobId: JobId
    val jobAttemptId: JobAttemptId
    val jobAttemptRetry: JobAttemptRetry
    val jobAttemptIndex: JobAttemptIndex
}
