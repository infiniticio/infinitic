package com.zenaton.pulsar.topics.tasks.dispatcher

import com.zenaton.engine.tasks.messages.TaskMessageInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.tasks.messages.TaskMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object TaskDispatcher {
    fun dispatch(context: Context, msg: TaskMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), JSONSchema.of(TaskMessageContainer::class.java))
            .key(msg.getKey())
            .value(TaskMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
