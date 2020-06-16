package com.zenaton.jobManager.messages.envelopes

import com.zenaton.jobManager.data.JobId

interface ForEngineMessage {
    val jobId: JobId
}
