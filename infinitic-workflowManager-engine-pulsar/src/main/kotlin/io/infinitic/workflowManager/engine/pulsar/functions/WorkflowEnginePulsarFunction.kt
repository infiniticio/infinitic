package io.infinitic.workflowManager.pulsar.functions

import io.infinitic.workflowManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.workflowManager.engine.avroEngines.AvroWorkflowEngine
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEnginePulsarFunction : Function<AvroEnvelopeForWorkflowEngine, Void> {

    var engine = AvroWorkflowEngine()

    override fun process(input: AvroEnvelopeForWorkflowEngine, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            engine.logger = ctx.logger
            engine.avroStorage = PulsarAvroStorage(ctx)
            engine.avroDispatcher = PulsarDispatcher.forPulsarFunctionContext(ctx)

            engine.handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
