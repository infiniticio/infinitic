package com.zenaton.api.task.messages.commands

import com.zenaton.api.task.messages.JobMessage
import java.time.Instant

data class DispatchJobCommand(val jobId: String, val jobName: String, val sentAt: Instant) : JobMessage {
    object Fields {
        const val JOB_NAME = "dispatchjob.jobname"
    }
}
