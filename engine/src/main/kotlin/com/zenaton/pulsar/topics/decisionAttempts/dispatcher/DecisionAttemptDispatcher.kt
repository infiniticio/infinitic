package com.zenaton.pulsar.topics.decisionAttempts.dispatcher

import com.zenaton.engine.decisionAttempts.messages.DecisionAttemptMessageInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisionAttempts.messages.DecisionAttemptMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object DecisionAttemptDispatcher {
    fun dispatch(context: Context, msg: DecisionAttemptMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.DECISION_ATTEMPTS.get(msg.getName()), JSONSchema.of(DecisionAttemptMessageContainer::class.java))
            .key(msg.getKey())
            .value(DecisionAttemptMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
