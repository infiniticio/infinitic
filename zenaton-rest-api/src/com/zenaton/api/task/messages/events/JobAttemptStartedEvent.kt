package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.JobMessage
import java.time.Instant

data class JobAttemptStartedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant) : JobMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptstarted.jobattemptid"
        const val ATTEMPT_RETRY = "jobattemptstarted.jobattemptretry"
        const val ATTEMPT_INDEX = "jobattemptstarted.jobattemptindex"
    }
}
