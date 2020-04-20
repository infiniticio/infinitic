package com.zenaton.pulsar.serializer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zenaton.engine.workflows.WorkflowState
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.messages.WorkflowMessage
import com.zenaton.pulsar.workflows.PulsarMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer

object MessageSerDe : MessageSerDeInterface {
    val mapper = jacksonObjectMapper()

    override fun fromPulsar(input: PulsarMessage): WorkflowMessage {
        val serialised = toJson(input)
        return when (input.type) {
            "WorkflowDispatched" -> fromJson<WorkflowDispatched>(serialised)
            "TaskCompleted" -> fromJson<TaskCompleted>(serialised)
            "DelayCompleted" -> fromJson<DelayCompleted>(serialised)
            "DecisionCompleted" -> fromJson<DecisionCompleted>(serialised)
            else -> throw Exception()
        }
    }

    override fun toPulsar(msg: WorkflowMessage): PulsarMessage {
        return fromJson<PulsarMessage>(toJson(msg))
    }

    inline fun <reified T> fromJson(json: String): T {
        return mapper.readValue<T>(json)
    }

    override fun toJson(msg: Any): String {
        return mapper.writeValueAsString(msg)
    }

    override fun deSerializeState(byteBuffer: ByteBuffer): WorkflowState {
        return deserialize<WorkflowState>(byteBuffer)
    }

    override fun serializeState(state: WorkflowState): ByteBuffer {
        return serialize(state)
    }

    private fun <T> serialize(value: T): ByteBuffer {
        val bos = ByteArrayOutputStream()
        val out = ObjectOutputStream(bos)

        out.writeObject(value)
        out.flush()

        return ByteBuffer.wrap(bos.toByteArray())
    }

    private inline fun <reified T> deserialize(data: ByteBuffer): T {
        val bis = ByteArrayInputStream(data.array())
        val ois = ObjectInputStream(bis)

        return ois.readObject() as T
    }
}
