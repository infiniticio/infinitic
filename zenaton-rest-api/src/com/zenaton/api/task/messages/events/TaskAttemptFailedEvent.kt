package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptFailedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant, val delayBeforeRetry: Float) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "taskattemptfailed.taskattemptid"
        const val ATTEMPT_INDEX = "taskattemptfailed.taskattemptindex"
        const val SENT_AT = "taskattemptfailed.sentat"
        const val DELAY_BEFORE_RETRY = "taskattemptfailed.delaybeforeretry"
    }
}
