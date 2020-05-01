package com.zenaton.pulsar.topics.delays

import com.zenaton.engine.topics.delays.DelayDispatcherInterface
import com.zenaton.engine.topics.delays.messages.DelayDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.delays.messages.DelayMessageContainer
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class DelayDispatcher(private val context: Context) : DelayDispatcherInterface {

    override fun dispatchDelay(msg: DelayDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DECISIONS.topic,
            JSONSchema.of(DelayMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(DelayMessageContainer(delayDispatched = msg))
            .send()
    }
}
