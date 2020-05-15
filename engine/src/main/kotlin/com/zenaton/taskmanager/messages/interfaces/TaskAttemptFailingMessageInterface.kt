package com.zenaton.taskmanager.messages.interfaces

interface TaskAttemptFailingMessageInterface : TaskAttemptMessageInterface {
    val taskAttemptDelayBeforeRetry: Float?
}
