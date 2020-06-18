package com.zenaton.workflowManager.pulsar.topics.delays.functions

import com.zenaton.workflowManager.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowManager.topics.delays.interfaces.DelayEngineDispatcherInterface
import com.zenaton.workflowManager.messages.DelayCompleted
import org.apache.pulsar.functions.api.Context

class DelayEngineDispatcher(private val context: Context) :
    DelayEngineDispatcherInterface {

    override fun dispatch(msg: DelayCompleted, after: Float) {
        WorkflowDispatcher(context).dispatch(msg, after)
    }
}
