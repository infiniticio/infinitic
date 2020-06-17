package com.zenaton.decisionmanager.pulsar.avro

import com.zenaton.commons.json.Json
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptCompleted
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptFailed
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptMessage
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptRetried
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptStarted
import com.zenaton.decisionmanager.messages.AvroDecisionAttemptTimeout
import com.zenaton.decisionmanager.messages.AvroDecisionDispatched
import com.zenaton.decisionmanager.messages.AvroDecisionMessage
import com.zenaton.decisionmanager.messages.AvroDecisionMessageType
import com.zenaton.decisionmanager.messages.DecisionAttemptCompleted
import com.zenaton.decisionmanager.messages.DecisionAttemptDispatched
import com.zenaton.decisionmanager.messages.DecisionAttemptFailed
import com.zenaton.decisionmanager.messages.DecisionAttemptRetried
import com.zenaton.decisionmanager.messages.DecisionAttemptStarted
import com.zenaton.decisionmanager.messages.DecisionAttemptTimeout
import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface
import com.zenaton.decisionmanager.state.DecisionState
import com.zenaton.decisionmanager.states.AvroDecisionState

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Decision State
     */
    fun toAvro(obj: DecisionState) = convert<AvroDecisionState>(obj)
    fun fromAvro(obj: AvroDecisionState) = convert<DecisionState>(obj)

    /**
     *  Decision Attempts Messages
     */
    fun toAvro(obj: DecisionAttemptDispatched) = convert<AvroDecisionAttemptMessage>(obj)
    fun fromAvro(obj: AvroDecisionAttemptMessage) = convert<DecisionAttemptDispatched>(obj)

    /**
     *  Decisions Messages
     */
    fun toAvro(obj: DecisionAttemptCompleted) = convert<AvroDecisionAttemptCompleted>(obj)
    fun fromAvro(obj: AvroDecisionAttemptCompleted) = convert<DecisionAttemptCompleted>(obj)

    fun toAvro(obj: DecisionAttemptFailed) = convert<AvroDecisionAttemptFailed>(obj)
    fun fromAvro(obj: AvroDecisionAttemptFailed) = convert<DecisionAttemptFailed>(obj)

    fun toAvro(obj: DecisionAttemptRetried) = convert<AvroDecisionAttemptRetried>(obj)
    fun fromAvro(obj: AvroDecisionAttemptRetried) = convert<DecisionAttemptRetried>(obj)

    fun toAvro(obj: DecisionAttemptStarted) = convert<AvroDecisionAttemptStarted>(obj)
    fun fromAvro(obj: AvroDecisionAttemptStarted) = convert<DecisionAttemptStarted>(obj)

    fun toAvro(obj: DecisionAttemptTimeout) = convert<AvroDecisionAttemptTimeout>(obj)
    fun fromAvro(obj: AvroDecisionAttemptTimeout) = convert<DecisionAttemptTimeout>(obj)

    fun toAvro(obj: DecisionDispatched) = convert<AvroDecisionDispatched>(obj)
    fun fromAvro(obj: AvroDecisionDispatched) = convert<DecisionDispatched>(obj)

    fun toAvro(msg: DecisionMessageInterface): AvroDecisionMessage {
        var builder = AvroDecisionMessage.newBuilder()
        builder.decisionId = msg.decisionId.id
        builder = when (msg) {
            is DecisionAttemptCompleted ->
                builder
                    .setDecisionAttemptCompleted(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionAttemptCompleted)
            is DecisionAttemptFailed ->
                builder
                    .setDecisionAttemptFailed(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionAttemptFailed)
            is DecisionAttemptRetried ->
                builder
                    .setDecisionAttemptRetried(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionAttemptRetried)
            is DecisionAttemptStarted ->
                builder
                    .setDecisionAttemptStarted(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionAttemptStarted)
            is DecisionAttemptTimeout ->
                builder
                    .setDecisionAttemptTimeout(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionAttemptTimeout)
            is DecisionDispatched ->
                builder
                    .setDecisionDispatched(toAvro(msg))
                    .setType(AvroDecisionMessageType.DecisionDispatched)
            else -> throw Exception("Unknown decision message class ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroDecisionMessage): DecisionMessageInterface {
        val type = input.getType()
        return when (type) {
            AvroDecisionMessageType.DecisionAttemptCompleted -> fromAvro(input.getDecisionAttemptCompleted())
            AvroDecisionMessageType.DecisionAttemptFailed -> fromAvro(input.getDecisionAttemptFailed())
            AvroDecisionMessageType.DecisionAttemptRetried -> fromAvro(input.getDecisionAttemptRetried())
            AvroDecisionMessageType.DecisionAttemptStarted -> fromAvro(input.getDecisionAttemptStarted())
            AvroDecisionMessageType.DecisionAttemptTimeout -> fromAvro(input.getDecisionAttemptTimeout())
            AvroDecisionMessageType.DecisionDispatched -> fromAvro(input.getDecisionDispatched())
            else -> throw Exception("Unknown avro decision message type: $type")
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse<T>(Json.stringify(from))
}
