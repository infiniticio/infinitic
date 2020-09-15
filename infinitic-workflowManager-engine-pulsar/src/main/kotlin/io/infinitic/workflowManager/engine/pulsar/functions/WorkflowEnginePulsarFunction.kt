package io.infinitic.workflowManager.pulsar.functions

import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.workflowManager.engine.storages.AvroKeyValueWorkflowStateStorage
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEnginePulsarFunction : Function<AvroEnvelopeForWorkflowEngine, Void> {

    override fun process(input: AvroEnvelopeForWorkflowEngine, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            val message = AvroConverter.fromWorkflowEngine(input)

            getWorkflowEngine(context).handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return@runBlocking null
    }

    internal fun getWorkflowEngine(context: Context): WorkflowEngine {
        val logger = context.logger
        val storage = AvroKeyValueWorkflowStateStorage(PulsarFunctionStorage(context))
        val dispatcher = Dispatcher(PulsarDispatcher.forPulsarFunctionContext(context))

        return WorkflowEngine(storage, dispatcher)
    }
}
