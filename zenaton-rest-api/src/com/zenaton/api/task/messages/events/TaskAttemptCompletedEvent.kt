package com.zenaton.api.task.messages.events

import com.zenaton.api.task.messages.TaskMessage
import java.time.Instant

data class TaskAttemptCompletedEvent(val attemptId: String, val attemptRetry: Int, val attemptIndex: Int, val sentAt: Instant) : TaskMessage {
    object Fields {
        const val ATTEMPT_ID = "jobattemptcompleted.jobattemptid"
        const val ATTEMPT_RETRY = "jobattemptcompleted.jobattemptretry"
        const val ATTEMPT_INDEX = "jobattemptcompleted.jobattemptindex"
    }
}
