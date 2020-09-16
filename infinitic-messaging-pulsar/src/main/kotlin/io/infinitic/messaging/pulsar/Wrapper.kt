package io.infinitic.messaging.pulsar

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder

interface Wrapper {
    fun <O> newMessage(topicName: String, schema: Schema<O>): TypedMessageBuilder<O>
}
