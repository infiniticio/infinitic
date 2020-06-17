package com.zenaton.jobManager.messages.envelopes

import com.zenaton.jobManager.data.JobId

interface ForJobEngineMessage {
    val jobId: JobId
}
