package io.infinitic.taskManager.worker.pulsar.functions

import io.infinitic.messaging.pulsar.PulsarDispatcher
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.taskManager.worker.WorkerDispatcher
import io.infinitic.taskManager.worker.Worker
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

open class TaskWorkerPulsarFunction : Function<AvroEnvelopeForWorker, Void> {

    override fun process(input: AvroEnvelopeForWorker, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            getWorker(context).handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return@runBlocking null
    }

    fun getWorker(context: Context): Worker {
        return Worker(WorkerDispatcher(PulsarDispatcher.forPulsarFunctionContext(context)))
    }
}
