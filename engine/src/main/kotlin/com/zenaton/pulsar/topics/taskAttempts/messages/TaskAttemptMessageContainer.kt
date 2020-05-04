package com.zenaton.pulsar.topics.taskAttempts.messages

import com.zenaton.engine.taskAttempts.messages.TaskAttemptInterface

class TaskAttemptMessageContainer(private val taskAttemptDispatched: TaskAttemptInterface) {
    fun msg(): TaskAttemptInterface = taskAttemptDispatched
}
