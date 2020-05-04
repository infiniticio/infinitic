package com.zenaton.pulsar.topics.decisionAttempts.messages

import com.zenaton.engine.decisionAttempts.messages.DecisionAttemptMessageInterface

class DecisionAttemptMessageContainer(private val decisionAttemptDispatched: DecisionAttemptMessageInterface) {
    fun msg(): DecisionAttemptMessageInterface = decisionAttemptDispatched
}
