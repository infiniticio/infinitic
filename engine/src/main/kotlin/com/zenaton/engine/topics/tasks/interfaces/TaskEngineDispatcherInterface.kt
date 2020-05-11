package com.zenaton.engine.topics.tasks.interfaces

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.topics.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.topics.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import org.apache.pulsar.client.api.MessageId

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: TaskCompleted): MessageId
    fun dispatch(msg: TaskAttemptRetried, after: Float = 0f): MessageId
    fun dispatch(msg: TaskAttemptTimeout, after: Float = 0f): MessageId
    fun dispatch(msg: TaskAttemptDispatched): MessageId
}
