package com.zenaton.pulsar.workflows

import com.zenaton.engine.topics.decisions.DecisionDispatched
import com.zenaton.engine.topics.delays.DelayDispatched
import com.zenaton.engine.topics.tasks.TaskDispatched
import com.zenaton.engine.topics.workflows.DispatcherInterface
import com.zenaton.engine.topics.workflows.WorkflowDispatched
import com.zenaton.pulsar.workflows.serializers.MessageConverter
import com.zenaton.pulsar.workflows.serializers.MessageConverterInterface
import org.apache.pulsar.functions.api.Context

class Dispatcher(private val context: Context) : DispatcherInterface {

    // MessageConverter injection
    var converter: MessageConverterInterface = MessageConverter

    override fun dispatchTask(msg: TaskDispatched) {
        TODO("Not yet implemented")
    }

    override fun dispatchWorkflow(msg: WorkflowDispatched) {
        TODO("Not yet implemented")
    }

    override fun dispatchDelay(msg: DelayDispatched) {
        TODO("Not yet implemented")
    }

    override fun dispatchDecision(msg: DecisionDispatched) {
        TODO("Not yet implemented")
    }
}
