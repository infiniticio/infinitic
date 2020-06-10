package com.zenaton.workflowManager.pulsar.topics.workflows.functions

import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.decisionmanager.pulsar.dispatcher.DecisionDispatcher
import com.zenaton.jobManager.messages.DispatchJob
// import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.workflowManager.pulsar.topics.delays.dispatcher.DelayDispatcher
import com.zenaton.workflowManager.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowManager.topics.delays.messages.DelayDispatched
import com.zenaton.workflowManager.topics.workflows.interfaces.WorkflowEngineDispatcherInterface
import com.zenaton.workflowManager.messages.WorkflowDispatched
import org.apache.pulsar.functions.api.Context

class WorkflowEngineDispatcher(private val context: Context) : WorkflowEngineDispatcherInterface {

    override fun dispatch(msg: DispatchJob, after: Float) {
//        PulsarAvroDispatcher(context).toEngine(AvroConverter.toEngine(msg), after)
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
