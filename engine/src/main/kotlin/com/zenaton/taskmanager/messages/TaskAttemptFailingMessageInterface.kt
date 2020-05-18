package com.zenaton.taskmanager.messages

interface TaskAttemptFailingMessageInterface :
    TaskAttemptMessageInterface {
    val taskAttemptDelayBeforeRetry: Float?
}
