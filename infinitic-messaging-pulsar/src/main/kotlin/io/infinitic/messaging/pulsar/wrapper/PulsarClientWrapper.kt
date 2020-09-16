package io.infinitic.messaging.pulsar.wrapper

import io.infinitic.messaging.pulsar.Wrapper
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder

class PulsarClientWrapper(private val client: PulsarClient) : Wrapper {
    private val producers = mutableMapOf<String, Producer<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <O> newMessage(topicName: String, schema: Schema<O>): TypedMessageBuilder<O> {
        val producer = producers[topicName]
        if (producer != null) {
            return producer.newMessage() as TypedMessageBuilder<O>
        }

        val newProducer = client
            .newProducer(schema)
            .topic(topicName)
            .create()

        producers[topicName] = newProducer

        return newProducer.newMessage()
    }
}
