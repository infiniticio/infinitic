package com.zenaton.engine.topics.tasks.interfaces

import com.zenaton.engine.data.TaskAttemptId

interface TaskAttemptFailingMessageInterface : TaskMessageInterface {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
    val taskAttemptDelayBeforeRetry: Float?
}
