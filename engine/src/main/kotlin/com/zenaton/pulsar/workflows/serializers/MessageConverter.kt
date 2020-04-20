package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.workflows.DecisionCompleted
import com.zenaton.engine.workflows.DelayCompleted
import com.zenaton.engine.workflows.TaskCompleted
import com.zenaton.engine.workflows.WorkflowDispatched
import com.zenaton.engine.workflows.WorkflowMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface
import com.zenaton.pulsar.workflows.PulsarMessage

object MessageConverter : MessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarMessage): WorkflowMessage {
        val serialised = json.to(input)
        return when (input.type) {
            "WorkflowDispatched" -> json.from(serialised, WorkflowDispatched::class) as WorkflowDispatched
            "TaskCompleted" -> json.from(serialised, TaskCompleted::class) as TaskCompleted
            "DelayCompleted" -> json.from(serialised, DelayCompleted::class) as DelayCompleted
            "DecisionCompleted" -> json.from(serialised, DecisionCompleted::class) as DecisionCompleted
            else -> throw Exception()
        }
    }

    override fun toPulsar(msg: WorkflowMessage): PulsarMessage {
        return json.from(json.to(msg), PulsarMessage::class) as PulsarMessage
    }
}
