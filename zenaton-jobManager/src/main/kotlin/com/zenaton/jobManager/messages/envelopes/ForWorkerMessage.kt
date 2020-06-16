package com.zenaton.jobManager.messages.envelopes

import com.zenaton.jobManager.data.JobName

interface ForWorkerMessage {
    val jobName: JobName
}
