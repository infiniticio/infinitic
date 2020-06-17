package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptDispatchedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "taskattemptdispatched.taskattemptid"
        const val ATTEMPT_INDEX = "taskattemptdispatched.taskattemptindex"
        const val SENT_AT = "taskattemptdispatched.sentat"
    }
}
