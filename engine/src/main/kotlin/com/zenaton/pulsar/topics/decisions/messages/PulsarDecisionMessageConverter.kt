package com.zenaton.pulsar.topics.decisions.messages

import com.zenaton.engine.topics.decisions.DecisionMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface

object PulsarDecisionMessageConverter : PulsarDecisionMessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarDecisionMessage): DecisionMessage {
        return json.parse(
            json.stringify(input), DecisionMessage::class) as DecisionMessage
    }

    override fun toPulsar(msg: DecisionMessage): PulsarDecisionMessage {
        return json.parse(
            json.stringify(msg), PulsarDecisionMessage::class) as PulsarDecisionMessage
    }
}
