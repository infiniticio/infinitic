package com.zenaton.pulsar.topics.taskAttempts.dispatcher

import com.zenaton.engine.taskAttempts.messages.TaskAttemptInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.taskAttempts.messages.TaskAttemptMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object TaskAttemptDispatcher {
    fun dispatch(context: Context, msg: TaskAttemptInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(), JSONSchema.of(TaskAttemptMessageContainer::class.java))
            .key(msg.getKey())
            .value(TaskAttemptMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
