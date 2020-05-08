package com.zenaton.engine.topics.decisions.interfaces

import com.zenaton.engine.topics.decisionAttempts.messages.DecisionAttemptDispatched
import com.zenaton.engine.topics.workflows.messages.DecisionCompleted

interface DecisionEngineDispatcherInterface {
    fun dispatch(msg: DecisionAttemptDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionCompleted, after: Float = 0f)
}
