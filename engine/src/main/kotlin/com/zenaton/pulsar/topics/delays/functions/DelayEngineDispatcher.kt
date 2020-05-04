package com.zenaton.pulsar.topics.delays.functions

import com.zenaton.engine.delays.engine.DelayEngineDispatcherInterface
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context

class DelayEngineDispatcher(private val context: Context) : DelayEngineDispatcherInterface {

    override fun dispatch(msg: DelayCompleted, after: Float) {
        WorkflowDispatcher.dispatch(context, msg, after)
    }
}
