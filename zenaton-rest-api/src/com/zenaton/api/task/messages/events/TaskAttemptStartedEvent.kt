package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptStartedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant, val delayBeforeRetry: Float, val delayBeforeTimeout: Float) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "taskattemptstarted.taskattemptid"
        const val ATTEMPT_INDEX = "taskattemptstarted.taskattemptindex"
        const val SENT_AT = "taskattemptstarted.sentat"
        const val DELAY_BEFORE_RETRY = "taskattemptstarted.taskattemptdelaybeforeretry"
        const val DELAY_BEFORE_TIMEOUT = "taskattemptstarted.taskattemptdelaybeforetimeout"
    }
}
