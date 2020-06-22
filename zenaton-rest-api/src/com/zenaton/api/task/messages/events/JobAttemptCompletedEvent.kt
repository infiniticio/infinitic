package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.JobMessage
import java.time.Instant

data class JobAttemptCompletedEvent(val attemptId: String, val attemptIndex: Int, val sentAt: Instant) : JobMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptcompleted.jobattemptid"
        const val ATTEMPT_INDEX = "jobattemptcompleted.jobattemptindex"
    }
}
