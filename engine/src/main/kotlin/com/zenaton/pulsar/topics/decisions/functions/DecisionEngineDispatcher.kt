package com.zenaton.pulsar.topics.decisions.functions

import com.zenaton.engine.decisionAttempts.messages.DecisionAttemptDispatched
import com.zenaton.engine.decisions.interfaces.DecisionEngineDispatcherInterface
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.pulsar.topics.decisionAttempts.dispatcher.DecisionAttemptDispatcher
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context

class DecisionEngineDispatcher(private val context: Context) :
    DecisionEngineDispatcherInterface {

    override fun dispatch(msg: DecisionAttemptDispatched, after: Float) {
        DecisionAttemptDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: DecisionCompleted, after: Float) {
        WorkflowDispatcher.dispatch(context, msg, after)
    }
}
