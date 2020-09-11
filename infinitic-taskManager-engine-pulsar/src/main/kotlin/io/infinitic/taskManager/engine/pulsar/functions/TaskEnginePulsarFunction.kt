package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.engine.storage.AvroKeyValueStateStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEnginePulsarFunction : Function<AvroEnvelopeForTaskEngine, Void> {

    var engine = TaskEngine()

    override fun process(input: AvroEnvelopeForTaskEngine, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        val message = AvroConverter.fromTaskEngine(input)

        try {
            engine.logger = ctx.logger
            engine.storage = AvroKeyValueStateStorage(PulsarFunctionStorage(ctx))
            engine.dispatcher = Dispatcher(PulsarDispatcher.forPulsarFunctionContext(ctx))

            engine.handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        null
    }
}
