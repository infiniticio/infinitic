package com.zenaton.pulsar.workflows.serializers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.messages.WorkflowMessage
import com.zenaton.pulsar.workflows.PulsarMessage

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
}
