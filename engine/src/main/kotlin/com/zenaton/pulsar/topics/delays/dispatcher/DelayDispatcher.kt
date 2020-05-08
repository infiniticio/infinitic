package com.zenaton.pulsar.topics.delays.dispatcher

import com.zenaton.engine.topics.delays.interfaces.DelayMessageInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.delays.messages.DelayMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object DelayDispatcher {
    fun dispatch(context: Context, msg: DelayMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.DELAYS.get(), JSONSchema.of(DelayMessageContainer::class.java))
            .key(msg.getKey())
            .value(DelayMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
