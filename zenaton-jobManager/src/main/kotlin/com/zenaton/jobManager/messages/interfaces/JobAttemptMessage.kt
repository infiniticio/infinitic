package com.zenaton.jobManager.messages.interfaces

import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobId

interface JobAttemptMessage {
    val jobId: JobId
    val jobAttemptId: JobAttemptId
    val jobAttemptRetry: JobAttemptRetry
    val jobAttemptIndex: JobAttemptIndex
}
