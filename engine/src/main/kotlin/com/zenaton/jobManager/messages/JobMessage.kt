package com.zenaton.jobManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId

interface JobMessage {
    val jobId: JobId
    val sentAt: DateTime
}
