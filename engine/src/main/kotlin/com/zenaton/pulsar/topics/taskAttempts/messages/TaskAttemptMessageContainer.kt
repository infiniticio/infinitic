package com.zenaton.pulsar.topics.taskAttempts.messages

import com.zenaton.engine.taskAttempts.interfaces.TaskAttemptMessageInterface

class TaskAttemptMessageContainer(private val taskAttemptDispatched: TaskAttemptMessageInterface) {
    fun msg(): TaskAttemptMessageInterface = taskAttemptDispatched
}
