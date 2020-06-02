package com.zenaton.jobManager.messages.interfaces

import com.zenaton.jobManager.data.JobAttemptId

interface JobAttemptMessage : JobMessage {
    val jobAttemptId: JobAttemptId
    val jobAttemptRetry: Int
    val jobAttemptIndex: Int
}
