package io.infinitic.messaging.pulsar

import io.infinitic.messaging.api.dispatcher.transport.AvroCompatibleTransport
import io.infinitic.messaging.pulsar.wrapper.PulsarClientWrapper
import io.infinitic.messaging.pulsar.wrapper.PulsarFunctionContextWrapper
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit

open class PulsarTransport constructor(protected val wrapper: Wrapper) : AvroCompatibleTransport {
    private var prefix = "tasks"

    fun usePrefix(newPrefix: String): PulsarTransport {
        prefix = newPrefix

        return this
    }

    override suspend fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        withContext(Dispatchers.IO) {
            val messageBuilder = wrapper
                .newMessage(Topic.WORKFLOW_ENGINE.get(prefix), AvroSchema.of(AvroEnvelopeForWorkflowEngine::class.java))
                .key(msg.workflowId)
                .value(msg)

            if (after > 0F) {
                messageBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
            }

            messageBuilder.send()
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

    override suspend fun toWorkers(msg: AvroEnvelopeForWorker) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.WORKERS.get(prefix, msg.taskName), AvroSchema.of(AvroEnvelopeForWorker::class.java))
                .value(msg)
                .send()
        }
    }

    companion object {
        private const val TOPIC_PREFIX = "topicPrefix"

        fun forPulsarClient(pulsarClient: PulsarClient): PulsarTransport = PulsarTransport(PulsarClientWrapper(pulsarClient))

        fun forPulsarFunctionContext(pulsarFunctionContext: Context): PulsarTransport {
            val dispatcher = PulsarTransport(PulsarFunctionContextWrapper(pulsarFunctionContext))
            val requestedPrefix = pulsarFunctionContext.getUserConfigValue(TOPIC_PREFIX)
            if (requestedPrefix.isPresent) {
                dispatcher.usePrefix(requestedPrefix.get().toString())
            }

            return dispatcher
        }
    }
}
