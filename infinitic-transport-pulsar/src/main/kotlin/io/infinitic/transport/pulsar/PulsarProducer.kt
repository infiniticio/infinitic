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

package io.infinitic.transport.pulsar

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.transport.pulsar.schemas.schemaDefinition
import mu.KotlinLogging
import org.apache.pulsar.client.api.BatcherBuilder
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerAccessMode
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal class PulsarProducer(val client: PulsarClient) {

    val logger = KotlinLogging.logger {}

    inline fun <T : Message, reified S : Envelope<T>> send(
        message: T,
        after: MillisDuration,
        topic: String,
        producerName: String,
        key: String? = null
    ) {
        @Suppress("UNCHECKED_CAST")
        val producer = getProducer<T, S>(topic, producerName, key)

        logger.debug { "Sending producerName='$producerName' after=$after key='$key' message='$message'" }

        producer
            .newMessage()
            .value(message.envelope())
            .also {
                if (key != null) {
                    it.key(key)
                }
                if (after > 0) {
                    it.deliverAfter(after.long, TimeUnit.MILLISECONDS)
                }
            }
            .send()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : Message, reified S : Envelope<T>> getProducer(topic: String, producerName: String, key: String?) =
        producers.computeIfAbsent(topic) {
            logger.debug { "Creating Producer with producerName='$producerName' topic='$topic'" }

            val schema: Schema<out Envelope<T>> = Schema.AVRO(schemaDefinition(S::class))

            client
                .newProducer(schema)
                .topic(topic)
                .producerName(producerName)
                .also {
                    if (key != null) {
                        it.batcherBuilder(BatcherBuilder.KEY_BASED)
                    }
                }
                .accessMode(ProducerAccessMode.Shared)
                .blockIfQueueFull(true)
                .create()
        } as Producer<Envelope<out Message>>

    companion object {
        val producers = ConcurrentHashMap<String, Producer<out Envelope<out Message>>>()
    }
}
