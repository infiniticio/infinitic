package io.infinitic.api.task.messages.events

import io.infinitic.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptDispatchedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "taskattemptdispatched.taskattemptid"
        const val ATTEMPT_RETRY = "taskattemptdispatched.taskattemptretry"
        const val ATTEMPT_INDEX = "taskattemptdispatched.taskattemptindex"
    }
}
