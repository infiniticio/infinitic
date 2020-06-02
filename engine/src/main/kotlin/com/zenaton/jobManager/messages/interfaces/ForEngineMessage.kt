package com.zenaton.jobManager.messages.interfaces

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId

interface ForEngineMessage {
    val jobId: JobId
    val sentAt: DateTime
}
