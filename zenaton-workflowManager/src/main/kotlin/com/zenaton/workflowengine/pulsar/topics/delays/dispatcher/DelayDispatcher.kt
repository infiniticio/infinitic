package com.zenaton.workflowengine.pulsar.topics.delays.dispatcher

import com.zenaton.workflowengine.pulsar.topics.Topic
import com.zenaton.workflowengine.pulsar.topics.delays.messages.DelayMessageContainer
import com.zenaton.workflowengine.topics.delays.interfaces.DelayMessageInterface
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit

object DelayDispatcher {
    fun dispatch(context: Context, msg: DelayMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.DELAYS.get(), JSONSchema.of(DelayMessageContainer::class.java))
            .key(msg.getStateId())
            .value(DelayMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
