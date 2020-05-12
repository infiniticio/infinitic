package com.zenaton.engine.topics.tasks.interfaces

interface TaskAttemptFailingMessageInterface : TaskAttemptMessageInterface {
    val taskAttemptDelayBeforeRetry: Float
}
