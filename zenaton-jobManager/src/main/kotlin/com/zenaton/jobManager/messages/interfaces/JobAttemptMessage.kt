package com.zenaton.jobManager.messages.interfaces

import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptIndex
import com.zenaton.jobManager.data.JobAttemptRetry

interface JobAttemptMessage : JobMessage {
    val jobAttemptId: JobAttemptId
    val jobAttemptRetry: JobAttemptRetry
    val jobAttemptIndex: JobAttemptIndex
}
