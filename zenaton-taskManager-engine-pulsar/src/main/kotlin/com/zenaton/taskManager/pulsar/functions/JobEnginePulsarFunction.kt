package com.zenaton.taskManager.pulsar.functions

import com.zenaton.taskManager.engine.avroClasses.AvroJobEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.taskManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.taskManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class JobEnginePulsarFunction : Function<AvroEnvelopeForJobEngine, Void> {

    var engine = AvroJobEngine()

    override fun process(input: AvroEnvelopeForJobEngine, context: Context?): Void? {
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
