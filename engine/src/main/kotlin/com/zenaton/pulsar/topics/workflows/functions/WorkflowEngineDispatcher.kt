package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.engine.decisions.messages.DecisionDispatched
import com.zenaton.engine.delays.messages.DelayDispatched
import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.workflows.functions.WorkflowEngineDispatcherInterface
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisions.dispatcher.DecisionDispatcher
import com.zenaton.pulsar.topics.decisions.messages.DecisionMessageContainer
import com.zenaton.pulsar.topics.delays.dispatcher.DelayDispatcher
import com.zenaton.pulsar.topics.delays.messages.DelayMessageContainer
import com.zenaton.pulsar.topics.tasks.dispatcher.TaskDispatcher
import com.zenaton.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class WorkflowEngineDispatcher(private val context: Context) : WorkflowEngineDispatcherInterface {

    override fun dispatch(msg: TaskDispatched, after: Float) {
        TaskDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: WorkflowDispatched, after: Float) {
        WorkflowDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: DelayDispatched, after: Float) {
        DelayDispatcher.dispatch(context, msg, after)
    }

    override fun dispatch(msg: DecisionDispatched, after: Float) {
        DecisionDispatcher.dispatch(context, msg, after)
    }
}
