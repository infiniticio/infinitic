package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.jobManager.data.JobId

interface ForTaskEngineMessage {
    val taskId: JobId
}
