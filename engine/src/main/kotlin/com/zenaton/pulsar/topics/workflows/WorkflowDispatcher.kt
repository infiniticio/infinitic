package com.zenaton.pulsar.topics.workflows

import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.delays.messages.DelayDispatched
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.engine.topics.workflows.WorkflowDispatcherInterface
import com.zenaton.engine.topics.workflows.messages.WorkflowDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.decisions.messages.DecisionMessageContainer
import com.zenaton.pulsar.topics.delays.messages.DelayMessageContainer
import com.zenaton.pulsar.topics.tasks.messages.TaskMessageContainer
import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

class WorkflowDispatcher(private val context: Context) : WorkflowDispatcherInterface {

    override fun dispatchTask(msg: TaskDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.TASKS.topic,
            JSONSchema.of(TaskMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(TaskMessageContainer(taskDispatched = msg))
            .send()
    }

    override fun dispatchChildWorkflow(msg: WorkflowDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.WORKFLOWS.topic,
            JSONSchema.of(WorkflowMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getStateKey())
            .value(WorkflowMessageContainer(workflowDispatched = msg))
            .send()
    }

    override fun dispatchDelay(msg: DelayDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DELAYS.topic,
            JSONSchema.of(DelayMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(DelayMessageContainer(delayDispatched = msg))
            .send()
    }

    override fun dispatchDecision(msg: DecisionDispatched) {
        val msgBuilder = context.newOutputMessage(
            Topic.DECISIONS.topic,
            JSONSchema.of(DecisionMessageContainer::class.java)
        )
        msgBuilder
            .key(msg.getKey())
            .value(DecisionMessageContainer(decisionDispatched = msg))
            .send()
    }
}
