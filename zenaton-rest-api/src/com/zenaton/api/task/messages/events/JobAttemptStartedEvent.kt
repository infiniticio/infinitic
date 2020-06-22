package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.JobMessage
import java.time.Instant

data class JobAttemptStartedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant, val delayBeforeRetry: Float, val delayBeforeTimeout: Float) : JobMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptstarted.jobattemptid"
        const val ATTEMPT_INDEX = "jobattemptstarted.jobattemptindex"
        const val DELAY_BEFORE_RETRY = "jobattemptstarted.jobattemptdelaybeforeretry"
        const val DELAY_BEFORE_TIMEOUT = "jobattemptstarted.jobattemptdelaybeforetimeout"
    }
}
