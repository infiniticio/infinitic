package io.infinitic.messaging.pulsar.wrapper

import io.infinitic.messaging.pulsar.Wrapper
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.functions.api.Context

class PulsarFunctionContextWrapper(private val context: Context) : Wrapper {
    override fun <O> newMessage(topicName: String, schema: Schema<O>): TypedMessageBuilder<O> = context.newOutputMessage(topicName, schema)
}
