package com.zenaton.decisionmanager.engine

import com.zenaton.decisionmanager.messages.DecisionAttemptDispatched
import com.zenaton.decisionmanager.messages.DecisionAttemptRetried
import com.zenaton.decisionmanager.messages.DecisionAttemptTimeout
import com.zenaton.workflowManager.messages.DecisionCompleted

interface DecisionEngineDispatcherInterface {
    fun dispatch(msg: DecisionCompleted)
    fun dispatch(msg: DecisionAttemptRetried, after: Float = 0f)
    fun dispatch(msg: DecisionAttemptTimeout, after: Float = 0f)
    fun dispatch(msg: DecisionAttemptDispatched)
}
