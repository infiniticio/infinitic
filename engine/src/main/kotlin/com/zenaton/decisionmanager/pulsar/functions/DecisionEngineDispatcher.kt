package com.zenaton.decisionmanager.pulsar.functions

import com.zenaton.decisionmanager.engine.DecisionEngineDispatcherInterface
import com.zenaton.decisionmanager.messages.DecisionAttemptDispatched
import com.zenaton.decisionmanager.messages.DecisionAttemptRetried
import com.zenaton.decisionmanager.messages.DecisionAttemptTimeout
import com.zenaton.decisionmanager.pulsar.dispatcher.DecisionDispatcher
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.workflows.messages.DecisionCompleted
import org.apache.pulsar.functions.api.Context

/**
 * This class provides a Pulsar implementation of 'DecisionEngineDispatcherInterface' used in DecisionEngine
 */
class DecisionEngineDispatcher(private val context: Context) : DecisionEngineDispatcherInterface {

    override fun dispatch(msg: DecisionAttemptDispatched) {
        DecisionDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: DecisionCompleted) {
        WorkflowDispatcher.dispatch(context, msg)
    }

    override fun dispatch(msg: DecisionAttemptRetried, after: Float) {
        DecisionDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: DecisionAttemptTimeout, after: Float) {
        DecisionDispatcher.dispatch(context, msg, after)
    }
}
