package com.zenaton.workflowManager.pulsar.functions

import com.zenaton.workflowManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.workflowManager.functions.WorkflowEngineFunction
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import com.zenaton.workflowManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEnginePulsarFunction : Function<AvroEnvelopeForWorkflowEngine, Void> {

    var engine = WorkflowEngineFunction()

    override fun process(input: AvroEnvelopeForWorkflowEngine, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            engine.logger = ctx.logger
            engine.avroStorage = PulsarAvroStorage(ctx)
            engine.avroDispatcher = PulsarAvroDispatcher(ctx)

            engine.handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
