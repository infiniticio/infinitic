package com.zenaton.taskManager.common.messages.interfaces

import com.zenaton.taskManager.common.data.JobAttemptId
import com.zenaton.taskManager.common.data.JobAttemptIndex
import com.zenaton.taskManager.common.data.JobAttemptRetry
import com.zenaton.taskManager.common.data.JobId

interface JobAttemptMessage {
    val jobId: JobId
    val jobAttemptId: JobAttemptId
    val jobAttemptRetry: JobAttemptRetry
    val jobAttemptIndex: JobAttemptIndex
}
