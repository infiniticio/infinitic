package com.zenaton.jobManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.WorkflowId

interface ForJobEngineMessage {
    val jobId: JobId
    val sentAt: DateTime
}
