package com.zenaton.pulsar.topics.decisions.messages

import com.zenaton.engine.topics.decisions.DecisionMessage

interface PulsarDecisionMessageConverterInterface {
    fun fromPulsar(input: PulsarDecisionMessage): DecisionMessage
    fun toPulsar(msg: DecisionMessage): PulsarDecisionMessage
}
