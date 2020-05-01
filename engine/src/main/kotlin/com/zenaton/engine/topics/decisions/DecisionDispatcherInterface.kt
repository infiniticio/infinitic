package com.zenaton.engine.topics.decisions

import com.zenaton.engine.topics.decisions.messages.DecisionAttemptDispatched

interface DecisionDispatcherInterface {
    fun dispatchDecisionAttempt(msg: DecisionAttemptDispatched)
}
