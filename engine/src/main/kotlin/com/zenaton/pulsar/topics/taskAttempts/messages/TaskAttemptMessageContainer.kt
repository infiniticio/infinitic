package com.zenaton.pulsar.topics.taskAttempts.messages

import com.zenaton.engine.taskAttempts.messages.TaskAttemptMessageInterface

class TaskAttemptMessageContainer(private val taskAttemptDispatched: TaskAttemptMessageInterface) {
    fun msg(): TaskAttemptMessageInterface = taskAttemptDispatched
}
