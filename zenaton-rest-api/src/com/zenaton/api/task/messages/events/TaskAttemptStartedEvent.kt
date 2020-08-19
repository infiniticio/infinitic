package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptStartedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptstarted.jobattemptid"
        const val ATTEMPT_RETRY = "jobattemptstarted.jobattemptretry"
        const val ATTEMPT_INDEX = "jobattemptstarted.jobattemptindex"
    }
}
