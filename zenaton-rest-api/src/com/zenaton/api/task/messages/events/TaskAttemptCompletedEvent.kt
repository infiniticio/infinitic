package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptCompletedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "taskattemptcompleted.taskattemptid"
        const val ATTEMPT_INDEX = "taskattemptcompleted.taskattemptindex"
    }
}
