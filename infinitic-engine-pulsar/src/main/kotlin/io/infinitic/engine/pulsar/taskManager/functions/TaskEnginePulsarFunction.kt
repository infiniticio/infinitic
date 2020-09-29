package io.infinitic.engine.pulsar.taskManager.functions

import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.messaging.pulsar.PulsarTransport
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.engine.taskManager.storage.AvroKeyValueTaskStateStorage
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEnginePulsarFunction : Function<AvroEnvelopeForTaskEngine, Void> {

    override fun process(input: AvroEnvelopeForTaskEngine, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            val message = AvroConverter.fromTaskEngine(input)

            getTaskEngine(ctx).handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        null
    }

    internal fun getTaskEngine(ctx: Context): TaskEngine {
        val storage = AvroKeyValueTaskStateStorage(PulsarFunctionStorage(ctx))
        val dispatcher = AvroDispatcher(PulsarTransport.forPulsarFunctionContext(ctx))

        return TaskEngine(storage, dispatcher)
    }
}
