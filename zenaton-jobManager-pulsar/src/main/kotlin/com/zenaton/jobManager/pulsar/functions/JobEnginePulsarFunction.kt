package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.functions.JobEngineFunction
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.jobManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class JobEnginePulsarFunction : Function<AvroForJobEngineMessage, Void> {

    var engine = JobEngineFunction()

    override fun process(input: AvroForJobEngineMessage, context: Context?): Void? {
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
