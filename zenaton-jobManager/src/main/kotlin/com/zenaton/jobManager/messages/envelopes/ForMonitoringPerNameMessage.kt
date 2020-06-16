package com.zenaton.jobManager.messages.envelopes

import com.zenaton.jobManager.data.JobName

interface ForMonitoringPerNameMessage {
    val jobName: JobName
}
