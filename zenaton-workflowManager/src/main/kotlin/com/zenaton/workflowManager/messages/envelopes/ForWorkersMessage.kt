package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.jobManager.data.JobId

interface ForWorkersMessage {
    val taskId: JobId
}
