package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.JobMessage
import java.time.Instant

data class JobAttemptFailedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant, val delayBeforeRetry: Float) : JobMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptfailed.jobattemptid"
        const val ATTEMPT_RETRY = "jobattemptfailed.jobattemptretry"
        const val ATTEMPT_INDEX = "jobattemptfailed.jobattemptindex"
        const val DELAY_BEFORE_RETRY = "jobattemptfailed.jobattemptdelaybeforeretry"
    }
}
