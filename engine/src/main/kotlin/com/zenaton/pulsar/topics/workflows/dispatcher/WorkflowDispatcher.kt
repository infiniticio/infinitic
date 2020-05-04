package com.zenaton.pulsar.topics.workflows.dispatcher

import com.zenaton.engine.workflows.messages.WorkflowMessageInterface
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object WorkflowDispatcher {
    fun dispatch(context: Context, msg: WorkflowMessageInterface, after: Float = 0f) {
        val msgBuilder = context
            .newOutputMessage(Topic.WORKFLOWS.get(), JSONSchema.of(WorkflowMessageContainer::class.java))
            .key(msg.getKey())
            .value(WorkflowMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
