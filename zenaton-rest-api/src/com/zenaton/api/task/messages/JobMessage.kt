package com.zenaton.api.task.messages

interface JobMessage {
    enum class Type {
        // Commands
        DISPATCH_JOB,
        RETRY_JOB,
        RETRY_JOB_ATTEMPT,
        TIMEOUT_JOB_ATTEMPT,
        // Events
        JOB_ATTEMPT_COMPLETED,
        JOB_ATTEMPT_DISPATCHED,
        JOB_ATTEMPT_FAILED,
        JOB_ATTEMPT_STARTED,
        ;

        companion object {
            fun fromString(type: String): Type = when (type) {
                "DispatchJob" -> DISPATCH_JOB
                "RetryJob" -> RETRY_JOB
                "RetryJobAttempt" -> RETRY_JOB_ATTEMPT
                "TimeoutJobAttempt" -> TIMEOUT_JOB_ATTEMPT
                "JobAttemptCompleted" -> JOB_ATTEMPT_COMPLETED
                "JobAttemptDispatched" -> JOB_ATTEMPT_DISPATCHED
                "JobAttemptFailed" -> JOB_ATTEMPT_FAILED
                "JobAttemptStarted" -> JOB_ATTEMPT_STARTED
                else -> throw IllegalArgumentException("Unknown task message type '$type'")
            }
        }
    }

    object Fields {
        const val JOB_ID = "jobid"
        const val TYPE = "type"
        const val SENT_AT = "__publish_time__";
    }
}
