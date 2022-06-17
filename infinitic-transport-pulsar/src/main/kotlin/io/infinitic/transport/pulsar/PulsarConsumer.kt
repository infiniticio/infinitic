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
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.DeadLetterPolicy
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.Message as PulsarMessage

internal class PulsarConsumer(val client: PulsarClient) {
    val logger = KotlinLogging.logger {}

    internal inline fun <T : Message, reified S : Envelope<out T>> CoroutineScope.startConsumer(
        crossinline executor: suspend (T) -> Unit,
        topic: String,
        subscriptionName: String,
        subscriptionType: SubscriptionType,
        consumerName: String,
        concurrency: Int,
        topicDLQ: String?,
        negativeAckRedeliveryDelaySeconds: Long = 30L,
        maxRedeliverCount: Int = 3
    ) {
        when (subscriptionType) {
            SubscriptionType.Key_Shared -> repeat(concurrency) {
                // For Key_Shared subscription, we need to create a new consumer for each executor coroutine
                launch {
                    @Suppress("UNCHECKED_CAST")
                    val consumer = createConsumer<T, S>(
                        topic = topic,
                        subscriptionName = subscriptionName, // subscriptionName MUST be the same for all concurrent instances!
                        subscriptionType = subscriptionType,
                        consumerName = "$consumerName-$it",
                        topicDLQ = topicDLQ,
                        negativeAckRedeliveryDelay = negativeAckRedeliveryDelaySeconds,
                        maxRedeliverCount = maxRedeliverCount
                    ) as Consumer<S>

                    while (isActive) {
                        // await() ensures this coroutine exits in case of throwable not caught
                        val pulsarMessage = try {
                            consumer.receiveAsync().await()
                        } catch (e: CancellationException) {
                            // Not sur it's the right way to handle this exception
                            negativeAcknowledge(consumer, consumer.lastMessageId)
                            break
                        } catch (e: Throwable) {
                            logger.error(e) { "Error when receiving message" }
                            throw e
                        }

                        val message = try {
                            pulsarMessage.value.message()
                        } catch (e: Exception) {
                            logger.error(e) { "${pulsarMessage.messageId}: Exception when deserializing $pulsarMessage" }
                            negativeAcknowledge(consumer, pulsarMessage.messageId)
                            continue
                        } catch (e: Throwable) {
                            logger.error(e) { "${pulsarMessage.messageId}: Throwable when deserializing $pulsarMessage" }
                            throw e
                        }

                        logger.debug { "Receiving consumerName='$consumerName-$it' messageId='${pulsarMessage.messageId}' key='${pulsarMessage.key}' message='$message'" }

                        try {
                            executor(message)
                            consumer.acknowledge(pulsarMessage.messageId)
                        } catch (e: Exception) {
                            logger.error(e) { "${pulsarMessage.messageId}: Exception when handling $message" }
                            negativeAcknowledge(consumer, pulsarMessage.messageId)
                            continue
                        } catch (e: Throwable) {
                            logger.error(e) { "${pulsarMessage.messageId}: Throwable when handling $message" }
                            throw e
                        }
                    }
                }
            }
            else -> {
                // For other subscription, we can use the same consumer for all executor coroutines
                val consumer = createConsumer<T, S>(
                    topic = topic,
                    subscriptionName = subscriptionName,
                    subscriptionType = subscriptionType,
                    consumerName = consumerName,
                    topicDLQ = topicDLQ,
                    negativeAckRedeliveryDelay = negativeAckRedeliveryDelaySeconds,
                    maxRedeliverCount = maxRedeliverCount
                )

                val channel = Channel<PulsarMessage<out Envelope<T>>>()

                // Channel is backpressure aware, so we can use it to send messages to the executor coroutines
                launch {
                    while (isActive) {
                        // await() ensures this coroutine exits in case of throwable not caught
                        try {
                            channel.send(consumer.receiveAsync().await())
                        } catch (e: CancellationException) {
                            // Not sur it's the right way to handle this exception
                            negativeAcknowledge(consumer, consumer.lastMessageId)
                            break
                        } catch (e: Throwable) {
                            logger.error(e) { "Error when receiving message" }
                            throw e
                        }
                    }
                }

                repeat(concurrency) {
                    launch {
                        for (pulsarMessage in channel) {
                            val message = try {
                                pulsarMessage.value.message()
                            } catch (e: Exception) {
                                logger.error(e) { "${pulsarMessage.messageId}: Exception when deserializing $pulsarMessage" }
                                negativeAcknowledge(consumer, pulsarMessage.messageId)
                                continue
                            } catch (e: Throwable) {
                                logger.error(e) { "${pulsarMessage.messageId}: Throwable when deserializing $pulsarMessage" }
                                throw e
                            }

                            try {
                                executor(message)
                                consumer.acknowledge(pulsarMessage.messageId)
                            } catch (e: Exception) {
                                logger.error(e) { "${pulsarMessage.messageId}: Exception when handling $message" }
                                negativeAcknowledge(consumer, pulsarMessage.messageId)
                                continue
                            } catch (e: Throwable) {
                                logger.error(e) { "${pulsarMessage.messageId}: Throwable when handling $message" }
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    private fun negativeAcknowledge(consumer: Consumer<*>, messageId: MessageId) {
        try {
            consumer.negativeAcknowledge(messageId)
        } catch (e: Exception) {
            logger.error(e) { "Exception when negativeAcknowledging $messageId" }
            // message will be timeout and be redelivered
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : Message, reified S : Envelope<out T>> createConsumer(
        topic: String,
        subscriptionName: String,
        subscriptionType: SubscriptionType,
        consumerName: String,
        topicDLQ: String?,
        negativeAckRedeliveryDelay: Long,
        maxRedeliverCount: Int
    ): Consumer<out Envelope<T>> {
        logger.debug { "Creating Consumer consumerName='$consumerName' subscriptionName='$subscriptionName' subscriptionType='$subscriptionType' topic='$topic'" }

        val schema = Schema.AVRO(schemaDefinition<S>())

        return client.newConsumer(schema)
            .topic(topic)
            .subscriptionType(subscriptionType)
            .subscriptionName(subscriptionName)
            .consumerName(consumerName)
            .negativeAckRedeliveryDelay(negativeAckRedeliveryDelay, TimeUnit.SECONDS)
            .also {
                if (topicDLQ != null) {
                    when (subscriptionType) {
                        SubscriptionType.Key_Shared, SubscriptionType.Shared -> it.deadLetterPolicy(
                            DeadLetterPolicy.builder()
                                .maxRedeliverCount(maxRedeliverCount)
                                .deadLetterTopic(topicDLQ)
                                .build()
                        )
                        else -> Unit
                    }
                }
            }
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()
            as Consumer<out Envelope<T>>
    }
}
