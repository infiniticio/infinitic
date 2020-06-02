package com.zenaton.workflowengine.pulsar.topics.workflows.functions

import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.decisionmanager.pulsar.dispatcher.DecisionDispatcher
import com.zenaton.jobManager.engine.DispatchJob
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.workflowengine.pulsar.topics.delays.dispatcher.DelayDispatcher
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.delays.messages.DelayDispatched
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowEngineDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.WorkflowDispatched
import org.apache.pulsar.functions.api.Context

class WorkflowEngineDispatcher(private val context: Context) : WorkflowEngineDispatcherInterface {

    override fun dispatch(msg: DispatchJob, after: Float) {
        PulsarDispatcher(context).toEngine(msg, after)
    }

    override fun dispatch(msg: WorkflowDispatched, after: Float) {
        WorkflowDispatcher(context).dispatch(msg, after)
    }

    override fun dispatch(msg: DelayDispatched, after: Float) {
        DelayDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: DecisionDispatched, after: Float) {
        DecisionDispatcher.dispatch(context, msg, after)
    }
}
