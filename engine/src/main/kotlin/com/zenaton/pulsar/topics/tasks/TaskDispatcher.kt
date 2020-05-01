package com.zenaton.pulsar.topics.tasks

import com.zenaton.engine.topics.tasks.TaskDispatcherInterface
import com.zenaton.engine.topics.tasks.messages.TaskAttemptDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.tasks.messages.TaskMessageContainer
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class TaskDispatcher(private val context: Context) : TaskDispatcherInterface {

    override fun dispatchTaskAttempt(msg: TaskAttemptDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DECISIONS.topic,
            JSONSchema.of(TaskMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(TaskMessageContainer(taskAttemptDispatched = msg))
            .send()
    }
}
