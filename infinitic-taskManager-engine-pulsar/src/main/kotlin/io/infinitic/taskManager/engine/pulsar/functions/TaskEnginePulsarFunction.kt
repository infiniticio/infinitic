package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.taskManager.engine.avroClasses.AvroTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.engine.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEnginePulsarFunction : Function<AvroEnvelopeForTaskEngine, Void> {

    var engine = AvroTaskEngine()

    override fun process(input: AvroEnvelopeForTaskEngine, context: Context?): Void? {
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
