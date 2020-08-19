package com.zenaton.workflowManager.pulsar.dispatcher

import com.zenaton.taskManager.pulsar.dispatcher.Topic as JobTopic
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
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

    override fun toDeciders(msg: AvroEnvelopeForJobEngine) {
        val msgBuilder = context
            .newOutputMessage(JobTopic.JOB_ENGINE.get("decisions"), AvroSchema.of(AvroEnvelopeForJobEngine::class.java))
            .key(msg.jobId)
            .value(msg)
            .send()

        context.logger.debug("== WorkflowManager: dispatching ==")
        context.logger.debug("To topic: ${JobTopic.JOB_ENGINE.get("decisions")}")
        context.logger.debug("Msg: $msg")
    }

    override fun toWorkers(msg: AvroEnvelopeForJobEngine) {
        val msgBuilder = context
            .newOutputMessage(JobTopic.JOB_ENGINE.get("tasks"), AvroSchema.of(AvroEnvelopeForJobEngine::class.java))
            .key(msg.jobId)
            .value(msg)
            .send()

        context.logger.debug("== WorkflowManager: dispatching ==")
        context.logger.debug("To topic: ${JobTopic.JOB_ENGINE.get("tasks")}")
        context.logger.debug("Msg: $msg")
    }
}
