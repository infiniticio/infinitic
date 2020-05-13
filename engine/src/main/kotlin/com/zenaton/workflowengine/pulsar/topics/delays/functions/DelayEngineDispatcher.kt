package com.zenaton.workflowengine.pulsar.topics.delays.functions

import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.delays.interfaces.DelayEngineDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.DelayCompleted
import org.apache.pulsar.functions.api.Context

class DelayEngineDispatcher(private val context: Context) :
    DelayEngineDispatcherInterface {

    override fun dispatch(msg: DelayCompleted, after: Float) {
        WorkflowDispatcher.dispatch(context, msg, after)
    }
}
