package com.zenaton.engine.decisions.engine

import com.zenaton.engine.decisionAttempts.messages.DecisionAttemptDispatched
import com.zenaton.engine.workflows.messages.DecisionCompleted

interface DecisionEngineDispatcherInterface {
    fun dispatch(msg: DecisionAttemptDispatched, after: Float = 0f)
    fun dispatch(msg: DecisionCompleted, after: Float = 0f)
}
