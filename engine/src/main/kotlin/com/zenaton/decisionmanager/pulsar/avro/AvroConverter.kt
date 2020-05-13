package com.zenaton.decisionmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
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
import kotlin.reflect.KClass

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Decision State
     */
    fun toAvro(obj: DecisionState) = convert(obj, AvroDecisionState::class)
    fun fromAvro(obj: AvroDecisionState) = convert(obj, DecisionState::class)

    /**
     *  Decision Attempts Messages
     */
    fun toAvro(obj: DecisionAttemptDispatched) = convert(obj, AvroDecisionAttemptMessage::class)
    fun fromAvro(obj: AvroDecisionAttemptMessage) = convert(obj, DecisionAttemptDispatched::class)

    /**
     *  Decisions Messages
     */
    fun toAvro(obj: DecisionAttemptCompleted) = convert(obj, AvroDecisionAttemptCompleted::class)
    fun fromAvro(obj: AvroDecisionAttemptCompleted) = convert(obj, DecisionAttemptCompleted::class)

    fun toAvro(obj: DecisionAttemptFailed) = convert(obj, AvroDecisionAttemptFailed::class)
    fun fromAvro(obj: AvroDecisionAttemptFailed) = convert(obj, DecisionAttemptFailed::class)

    fun toAvro(obj: DecisionAttemptRetried) = convert(obj, AvroDecisionAttemptRetried::class)
    fun fromAvro(obj: AvroDecisionAttemptRetried) = convert(obj, DecisionAttemptRetried::class)

    fun toAvro(obj: DecisionAttemptStarted) = convert(obj, AvroDecisionAttemptStarted::class)
    fun fromAvro(obj: AvroDecisionAttemptStarted) = convert(obj, DecisionAttemptStarted::class)

    fun toAvro(obj: DecisionAttemptTimeout) = convert(obj, AvroDecisionAttemptTimeout::class)
    fun fromAvro(obj: AvroDecisionAttemptTimeout) = convert(obj, DecisionAttemptTimeout::class)

    fun toAvro(obj: DecisionDispatched) = convert(obj, AvroDecisionDispatched::class)
    fun fromAvro(obj: AvroDecisionDispatched) = convert(obj, DecisionDispatched::class)

    fun toAvro(msg: DecisionMessageInterface): AvroDecisionMessage {
        var builder = AvroDecisionMessage.newBuilder()
        builder.decisionId = msg.decisionId.id
        builder = when (msg) {
            is DecisionAttemptCompleted -> builder
                .setDecisionAttemptCompleted(toAvro(msg))
                .setType(AvroDecisionMessageType.DecisionAttemptCompleted)
            is DecisionAttemptFailed -> builder
                .setDecisionAttemptFailed(toAvro(msg))
                .setType(AvroDecisionMessageType.DecisionAttemptFailed)
            is DecisionAttemptRetried -> builder
                .setDecisionAttemptRetried(toAvro(msg))
                .setType(AvroDecisionMessageType.DecisionAttemptRetried)
            is DecisionAttemptStarted -> builder
                .setDecisionAttemptStarted(toAvro(msg))
                .setType(AvroDecisionMessageType.DecisionAttemptStarted)
            is DecisionAttemptTimeout -> builder
                .setDecisionAttemptTimeout(toAvro(msg))
                .setType(AvroDecisionMessageType.DecisionAttemptTimeout)
            is DecisionDispatched -> builder
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
    private fun <T : Any> convert(from: Any, to: KClass<T>): T = Json.parse(Json.stringify(from), to)
}
