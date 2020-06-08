package com.zenaton.decisionmanager.pulsar.dispatcher

import com.zenaton.decisionmanager.messages.AvroDecisionAttemptMessage
import com.zenaton.decisionmanager.messages.AvroDecisionMessage
import com.zenaton.decisionmanager.messages.DecisionAttemptDispatched
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface
import com.zenaton.decisionmanager.pulsar.Topic
import com.zenaton.decisionmanager.pulsar.avro.AvroConverter
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a decision message into the decisions topic
 */
object DecisionDispatcher {

    fun dispatch(context: Context, msg: DecisionAttemptDispatched): MessageId {
        return context
            .newOutputMessage(Topic.DECISIONS_ATTEMPTS.get(msg.decisionName.name), AvroSchema.of(AvroDecisionAttemptMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    fun dispatch(context: Context, msg: DecisionMessageInterface, after: Float = 0f): MessageId {

        val msgBuilder = context
            .newOutputMessage(Topic.DECISIONS.get(), AvroSchema.of(AvroDecisionMessage::class.java))
            .key(msg.getStateId())
            .value(AvroConverter.toAvro(msg))

        if (after > 0f) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        return msgBuilder.send()
    }
}
