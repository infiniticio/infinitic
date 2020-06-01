package com.zenaton.taskManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.JobId

interface JobMessage {
    val jobId: JobId
    val sentAt: DateTime
}
