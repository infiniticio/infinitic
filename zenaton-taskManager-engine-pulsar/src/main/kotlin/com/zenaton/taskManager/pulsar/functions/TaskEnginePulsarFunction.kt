package com.zenaton.taskManager.pulsar.functions

import com.zenaton.taskManager.engine.avroClasses.AvroTaskEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import com.zenaton.taskManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.taskManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEnginePulsarFunction : Function<AvroEnvelopeForTaskEngine, Void> {

    var engine = AvroTaskEngine()

    override fun process(input: AvroEnvelopeForTaskEngine, context: Context?): Void? {
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
