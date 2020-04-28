package com.zenaton.pulsar.topics.workflows

import com.zenaton.engine.topics.decisions.DecisionDispatched
import com.zenaton.engine.topics.delays.DelayDispatched
import com.zenaton.engine.topics.tasks.TaskDispatched
import com.zenaton.engine.topics.workflows.DispatcherInterface
import com.zenaton.engine.topics.workflows.WorkflowDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisions.messages.PulsarDecisionMessage
import com.zenaton.pulsar.topics.decisions.messages.PulsarDecisionMessageConverter
import com.zenaton.pulsar.topics.decisions.messages.PulsarDecisionMessageConverterInterface
import com.zenaton.pulsar.topics.delays.messages.PulsarDelayMessage
import com.zenaton.pulsar.topics.delays.messages.PulsarDelayMessageConverter
import com.zenaton.pulsar.topics.delays.messages.PulsarDelayMessageConverterInterface
import com.zenaton.pulsar.topics.tasks.messages.PulsarTaskMessage
import com.zenaton.pulsar.topics.tasks.messages.PulsarTaskMessageConverter
import com.zenaton.pulsar.topics.tasks.messages.PulsarTaskMessageConverterInterface
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessage
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessageConverter
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessageConverterInterface
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class Dispatcher(private val context: Context) : DispatcherInterface {

    // MessageConverters injection
    var workflowConverter: PulsarWorkflowMessageConverterInterface = PulsarWorkflowMessageConverter
    var taskConverter: PulsarTaskMessageConverterInterface = PulsarTaskMessageConverter
    var delayConverter: PulsarDelayMessageConverterInterface = PulsarDelayMessageConverter
    var decisionConverter: PulsarDecisionMessageConverterInterface = PulsarDecisionMessageConverter

    override fun dispatchTask(msg: TaskDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.TASKS.topic,
            JSONSchema.of(PulsarTaskMessage::class.java)
        )
        msgBuilder
            .key(msg.getStateKey())
            .value(taskConverter.toPulsar(msg))
            .send()
    }

    override fun dispatchChildWorkflow(msg: WorkflowDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.WORKFLOWS.topic,
            JSONSchema.of(PulsarWorkflowMessage::class.java)
        )
        msgBuilder
            .key(msg.getStateKey())
            .value(workflowConverter.toPulsar(msg))
            .send()
    }

    override fun dispatchDelay(msg: DelayDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DELAYS.topic,
            JSONSchema.of(PulsarDelayMessage::class.java)
        )
        msgBuilder
            .key(msg.getStateKey())
            .value(delayConverter.toPulsar(msg))
            .send()
    }

    override fun dispatchDecision(msg: DecisionDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DECISIONS.topic,
            JSONSchema.of(PulsarDecisionMessage::class.java)
        )
        msgBuilder
            .key(msg.getStateKey())
            .value(decisionConverter.toPulsar(msg))
            .send()
    }
}
