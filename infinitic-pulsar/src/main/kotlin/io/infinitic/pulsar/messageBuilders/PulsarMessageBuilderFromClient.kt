/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.messageBuilders

import org.apache.pulsar.client.api.BatcherBuilder
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder
import java.util.concurrent.ConcurrentHashMap

class PulsarMessageBuilderFromClient(
    private val pulsarClient: PulsarClient,
    private val producerName: String?
) : PulsarMessageBuilder {

    /*
    Store of exiting producers Map(topic name -> Pulsar producer)
     */
    companion object {
        private val producers = ConcurrentHashMap<String, Producer<*>>()
    }

    override fun <O> newMessage(topicName: String, schema: Schema<O>): TypedMessageBuilder<O> =
        getOrCreateProducer(topicName, schema).newMessage()

    @Suppress("UNCHECKED_CAST")
    fun <O> getOrCreateProducer(topicName: String, schema: Schema<O>) = producers.computeIfAbsent(topicName) {
        pulsarClient
            .newProducer(schema)
            .topic(topicName)
            .also {
                if (producerName != null) {
                    it.producerName(producerName)
                }
            }
            // adding this below is important - without it keyShared guarantees are broken
            // https://pulsar.apache.org/docs/en/client-libraries-java/#key_shared
            .batcherBuilder(BatcherBuilder.KEY_BASED)
            .create()
    } as Producer<O>
}
