package io.infinitic.taskManager.worker.pulsar.functions

import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.taskManager.worker.Dispatcher
import io.infinitic.taskManager.worker.Worker
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

open class TaskWorkerPulsarFunction : Function<AvroEnvelopeForWorker, Void> {
    var worker = Worker()

    override fun process(input: AvroEnvelopeForWorker, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            worker.dispatcher = Dispatcher(PulsarDispatcher.forPulsarFunctionContext(ctx))
            worker.handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return@runBlocking null
    }
}
