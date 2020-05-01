package com.zenaton.pulsar.topics.decisions

import com.zenaton.engine.topics.decisions.DecisionDispatcherInterface
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisions.messages.DecisionMessageContainer
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class DecisionDispatcher(private val context: Context) : DecisionDispatcherInterface {

    override fun dispatchDecisionAttempt(msg: DecisionAttemptDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DECISIONS.topic,
            JSONSchema.of(DecisionMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(DecisionMessageContainer(decisionAttemptDispatched = msg))
            .send()
    }
}
