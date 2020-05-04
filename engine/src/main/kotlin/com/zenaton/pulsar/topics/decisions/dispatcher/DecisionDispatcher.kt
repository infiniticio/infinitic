package com.zenaton.pulsar.topics.decisions.dispatcher

import com.zenaton.engine.decisions.messages.DecisionMessageInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisions.messages.DecisionMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object DecisionDispatcher {
    fun dispatch(context: Context, msg: DecisionMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.DECISIONS.get(), JSONSchema.of(DecisionMessageContainer::class.java))
            .key(msg.getKey())
            .value(DecisionMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
