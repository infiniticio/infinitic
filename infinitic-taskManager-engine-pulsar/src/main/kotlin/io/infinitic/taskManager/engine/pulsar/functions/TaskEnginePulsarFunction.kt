package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.taskManager.engine.dispatcher.AvroDispatcher
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.engine.storage.AvroKeyValueTaskStateStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
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
        val dispatcher = AvroDispatcher(PulsarDispatcher.forPulsarFunctionContext(ctx))

        return TaskEngine(storage, dispatcher)
    }
}
