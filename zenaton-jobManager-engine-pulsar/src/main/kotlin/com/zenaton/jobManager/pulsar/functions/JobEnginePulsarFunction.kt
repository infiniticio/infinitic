package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.engine.avroClasses.AvroJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.jobManager.pulsar.storage.PulsarAvroStorage
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
