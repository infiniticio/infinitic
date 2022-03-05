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

import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.transport.pulsar.schemas.schemaDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.Message as PulsarMessage

internal class PulsarListener(val client: PulsarClient) {
    val logger = KotlinLogging.logger {}

    inline fun <T : Message, reified S : Envelope<T>> CoroutineScope.start(
        crossinline executor: suspend (T) -> Unit,
        topic: String,
        subscriptionType: SubscriptionType,
        consumerName: String,
        concurrency: Int = 1,
        subscriptionName: String = consumerName,
        negativeAckRedeliveryDelay: Long = 30L,
        ackTimeout: Long = 60L
    ) = launch {

        when (subscriptionType) {
            SubscriptionType.Key_Shared -> repeat(concurrency) {

                // For Key_Shared subscription, we need to create a new consumer for each executor coroutine
                val consumer = createConsumer<T, S>(
                    topic,
                    subscriptionType,
                    "$subscriptionName-$it",
                    "$consumerName-$it",
                    negativeAckRedeliveryDelay,
                    ackTimeout
                )

                launch {
                    while (isActive) {
                        val pulsarMessage = consumer
                            .receive()

                        val message = try {
                            pulsarMessage.value.message()
                        } catch (e: Throwable) {
                            logger.error(e) { "exception when deserializing $pulsarMessage" }
                            consumer.negativeAcknowledge(pulsarMessage.messageId)
                            continue
                        }

                        try {
                            executor(message)
                        } catch (e: Throwable) {
                            logger.error(e) { "exception when handling $message" }
                            consumer.negativeAcknowledge(pulsarMessage.messageId)
                            continue
                        }
                        consumer.acknowledge(pulsarMessage.messageId)
                    }
                }
            }
            else -> {
                val channel = Channel<PulsarMessage<out Envelope<T>>>()

                // For other subscription, we can use the same consumer for all executor coroutines
                val consumer = createConsumer<T, S>(
                    topic,
                    subscriptionType,
                    subscriptionName,
                    consumerName,
                    negativeAckRedeliveryDelay,
                    ackTimeout
                )

                repeat(concurrency) {
                    launch {
                        for (pulsarMessage in channel) {

                            val message = try {
                                pulsarMessage.value.message()
                            } catch (e: Throwable) {
                                logger.error(e) { "exception when deserializing $pulsarMessage" }
                                consumer.negativeAcknowledge(pulsarMessage.messageId)
                                continue
                            }

                            try {
                                executor(message)
                            } catch (e: Throwable) {
                                logger.error(e) { "exception when handling $message" }
                                consumer.negativeAcknowledge(pulsarMessage.messageId)
                                continue
                            }
                            consumer.acknowledge(pulsarMessage.messageId)
                        }
                    }
                }

                // Channel is backpressure aware, so we can use it to send messages to the executor coroutines
                while (isActive) {
                    channel.send(consumer.receive())
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : Message, reified S : Envelope<T>> createConsumer(
        topic: String,
        subscriptionType: SubscriptionType,
        subscriptionName: String,
        consumerName: String,
        negativeAckRedeliveryDelay: Long,
        ackTimeout: Long,
    ): Consumer<out Envelope<T>> {
        val schema: Schema<out Envelope<T>> = Schema.AVRO(schemaDefinition(S::class))

        return client.newConsumer(schema)
            .topic(topic)
            .consumerName(consumerName)
            .subscriptionName(subscriptionName)
            .subscriptionType(subscriptionType)
            .ackTimeout(ackTimeout, TimeUnit.SECONDS)
            .negativeAckRedeliveryDelay(negativeAckRedeliveryDelay, TimeUnit.SECONDS)
            .subscribe()
            as Consumer<out Envelope<T>>
    }
}
