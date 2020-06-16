package com.zenaton.jobManager.messages.interfaces

import com.zenaton.jobManager.data.JobId

interface JobMessage {
    val jobId: JobId
}
