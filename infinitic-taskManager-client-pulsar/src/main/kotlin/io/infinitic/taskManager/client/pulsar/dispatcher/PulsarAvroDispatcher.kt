package io.infinitic.taskManager.client.pulsar.dispatcher

import io.infinitic.taskManager.client.AvroTaskDispatcher
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema

class PulsarAvroDispatcher(private val pulsarClient: PulsarClient) : AvroTaskDispatcher {
    private val producers = mutableMapOf<String, Producer<AvroEnvelopeForTaskEngine>>()

    override suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine) {
        withContext(Dispatchers.IO) {
            createOrGetProducer(Topic.TASK_ENGINE.get("tasks"))
                .newMessage()
                .key(msg.taskId)
                .value(msg)
                .send()
        }
    }

    private fun createOrGetProducer(topic: String): Producer<AvroEnvelopeForTaskEngine> {
        val existingProducer = producers[topic]
        if (existingProducer != null) {
            return existingProducer
        }

        val newProducer = pulsarClient
            .newProducer(AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
            .topic(topic)
            .create()

        producers[topic] = newProducer

        return newProducer
    }
}
