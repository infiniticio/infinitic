package com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher

import com.zenaton.workflowengine.pulsar.topics.Topic
import com.zenaton.workflowengine.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object WorkflowDispatcher {
    fun dispatch(context: Context, msg: WorkflowMessageInterface, after: Float = 0f): MessageId {
        val msgBuilder = context
            .newOutputMessage(Topic.WORKFLOWS.get(), JSONSchema.of(WorkflowMessageContainer::class.java))
            .key(msg.getStateId())
            .value(WorkflowMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        return msgBuilder.send()
    }
}
