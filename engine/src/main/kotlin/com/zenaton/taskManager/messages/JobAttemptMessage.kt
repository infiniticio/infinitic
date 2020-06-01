package com.zenaton.taskManager.messages

import com.zenaton.taskManager.data.JobAttemptId

interface JobAttemptMessage : JobMessage {
    val jobAttemptId: JobAttemptId
    val jobAttemptIndex: Int
}
