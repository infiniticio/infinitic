package com.zenaton.api.task.messages.commands

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class DispatchTaskCommand(val jobId: String, val jobName: String, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val JOB_NAME = "dispatchjob.jobname"
    }
}
