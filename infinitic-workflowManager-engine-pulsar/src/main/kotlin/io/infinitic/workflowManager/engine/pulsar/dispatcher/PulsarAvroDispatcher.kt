package com.zenaton.workflowManager.engine.pulsar.dispatcher

import io.infinitic.taskManager.engine.pulsar.dispatcher.Topic as TaskTopic
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.workflowManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit

class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {
    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        val msgBuilder = context
            .newOutputMessage(Topic.WORKFLOW_ENGINE.get(), AvroSchema.of(AvroEnvelopeForWorkflowEngine::class.java))
            .key(msg.workflowId)
            .value(msg)

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()

        context.logger.debug("== WorkflowManager: dispatching ==")
        context.logger.debug("To topic: ${Topic.WORKFLOW_ENGINE.get()}")
        context.logger.debug("Msg: $msg")
    }

    override fun toDeciders(msg: AvroEnvelopeForTaskEngine) {
        val msgBuilder = context
            .newOutputMessage   (TaskTopic.TASK_ENGINE.get("decisions"), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
            .key(msg.taskId)
            .value(msg)
            .send()

        context.logger.debug("== WorkflowManager: dispatching ==")
        context.logger.debug("To topic: ${TaskTopic.TASK_ENGINE.get("decisions")}")
        context.logger.debug("Msg: $msg")
    }

    override fun toWorkers(msg: AvroEnvelopeForTaskEngine) {
        val msgBuilder = context
            .newOutputMessage(TaskTopic.TASK_ENGINE.get("tasks"), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
            .key(msg.taskId)
            .value(msg)
            .send()

        context.logger.debug("== WorkflowManager: dispatching ==")
        context.logger.debug("To topic: ${TaskTopic.TASK_ENGINE.get("tasks")}")
        context.logger.debug("Msg: $msg")
    }
}
