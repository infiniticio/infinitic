package io.infinitic.api.task.messages.commands

import io.infinitic.api.task.messages.TaskMessage
import java.time.Instant

data class DispatchTaskCommand(val taskId: String, val taskName: String, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val JOB_NAME = "dispatchtask.taskname"
    }
}
