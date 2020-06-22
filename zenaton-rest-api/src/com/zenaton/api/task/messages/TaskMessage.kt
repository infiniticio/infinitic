package com.zenaton.api.task.messages

interface TaskMessage {
    enum class Type {
        // Commands
        DISPATCH_TASK,
        RETRY_TASK,
        RETRY_TASK_ATTEMPT,
        TIMEOUT_TASK_ATTEMPT,
        // Events
        TASK_ATTEMPT_COMPLETED,
        TASK_ATTEMPT_DISPATCHED,
        TASK_ATTEMPT_FAILED,
        TASK_ATTEMPT_STARTED,
        ;

        companion object {
            fun fromString(type: String): Type = when (type) {
                "DispatchTask" -> DISPATCH_TASK
                "RetryTask" -> RETRY_TASK
                "RetryTaskAttempt" -> RETRY_TASK_ATTEMPT
                "TimeoutTaskAttempt" -> TIMEOUT_TASK_ATTEMPT
                "TaskAttemptCompleted" -> TASK_ATTEMPT_COMPLETED
                "TaskAttemptDispatched" -> TASK_ATTEMPT_DISPATCHED
                "TaskAttemptFailed" -> TASK_ATTEMPT_FAILED
                "TaskAttemptStarted" -> TASK_ATTEMPT_STARTED
                else -> throw IllegalArgumentException("Unknown task message type '$type'")
            }
        }
    }

    object Fields {
        const val TASK_ID = "taskid"
        const val TYPE = "type"
        const val SENT_AT = "__publish_time__";
    }
}
