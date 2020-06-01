package com.zenaton.jobManager.messages

import com.zenaton.jobManager.data.JobAttemptId

interface JobAttemptMessage : JobMessage {
    val jobAttemptId: JobAttemptId
    val jobAttemptIndex: Int
}
