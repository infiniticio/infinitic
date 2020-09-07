package io.infinitic.taskManager.dispatcher.pulsar

import io.infinitic.taskManager.dispatcher.pulsar.wrapper.PulsarClientWrapper
import io.infinitic.taskManager.dispatcher.pulsar.wrapper.PulsarFunctionContextWrapper
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit
import io.infinitic.taskManager.client.AvroTaskDispatcher as AvroClientDispatcher
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher as AvroTaskEngineDispatcher
import io.infinitic.taskManager.worker.AvroDispatcher as AvroWorkerDispatcher

open class PulsarDispatcher constructor(protected val wrapper: Wrapper) : AvroClientDispatcher, AvroTaskEngineDispatcher, AvroWorkerDispatcher {
    private var prefix = "tasks"

    fun usePrefix(newPrefix: String): PulsarDispatcher {
        prefix = newPrefix

        return this
    }

    override suspend fun toWorkers(msg: AvroEnvelopeForWorker) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.WORKERS.get(prefix, msg.taskName), AvroSchema.of(AvroEnvelopeForWorker::class.java))
                .value(msg)
                .send()
        }
    }

    override suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.TASK_ENGINE.get(prefix), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
                .key(msg.taskId)
                .value(msg)
                .send()
        }
    }

    override suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float) {
        withContext(Dispatchers.IO) {
            val messageBuilder = wrapper
                .newMessage(Topic.TASK_ENGINE.get(prefix), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
                .key(msg.taskId)
                .value(msg)

            if (after > 0F) {
                messageBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
            }

            messageBuilder.send()
        }
    }

    override suspend fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.MONITORING_GLOBAL.get(prefix), AvroSchema.of(AvroEnvelopeForMonitoringGlobal::class.java))
                .value(msg)
                .send()
        }
    }

    override suspend fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.MONITORING_PER_NAME.get(prefix), AvroSchema.of(AvroEnvelopeForMonitoringPerName::class.java))
                .key(msg.taskName)
                .value(msg)
                .send()
        }
    }

    companion object {
        fun forPulsarClient(pulsarClient: PulsarClient): PulsarDispatcher = PulsarDispatcher(PulsarClientWrapper(pulsarClient))
        fun forPulsarFunctionContext(pulsarFunctionContext: Context): PulsarDispatcher {
            val dispatcher = PulsarDispatcher(PulsarFunctionContextWrapper(pulsarFunctionContext))
            val requestedPrefix = pulsarFunctionContext.getUserConfigValue("topicPrefix")
            if (requestedPrefix.isPresent) {
                dispatcher.usePrefix(requestedPrefix.get().toString())
            }

            return dispatcher
        }
    }
}
