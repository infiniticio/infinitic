package com.zenaton.api.task.messages.commands

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class DispatchTaskCommand(val taskId: String, val taskName: String, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val TASK_NAME = "dispatchtask.taskname"
        const val SENT_AT = "dispatchtask.sentat"
    }
}
