package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptFailedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant, val delayBeforeRetry: Float) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptfailed.jobattemptid"
        const val ATTEMPT_RETRY = "jobattemptfailed.jobattemptretry"
        const val ATTEMPT_INDEX = "jobattemptfailed.jobattemptindex"
        const val DELAY_BEFORE_RETRY = "jobattemptfailed.jobattemptdelaybeforeretry"
    }
}
