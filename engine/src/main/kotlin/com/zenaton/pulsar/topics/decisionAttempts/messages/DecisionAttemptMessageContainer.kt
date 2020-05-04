package com.zenaton.pulsar.topics.decisionAttempts.messages

import com.zenaton.engine.decisionAttempts.messages.DecisionAttemptInterface

class DecisionAttemptMessageContainer(private val decisionAttemptDispatched: DecisionAttemptInterface) {
    fun msg(): DecisionAttemptInterface = decisionAttemptDispatched
}
