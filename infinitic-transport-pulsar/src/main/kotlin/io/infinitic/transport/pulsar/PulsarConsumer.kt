/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
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
import io.infinitic.transport.pulsar.config.topics.ConsumerConfig
import io.infinitic.transport.pulsar.schemas.schemaDefinition
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.DeadLetterPolicy
import org.apache.pulsar.client.api.Message as PulsarMessage
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType

internal class PulsarConsumer(val client: PulsarClient, val config: ConsumerConfig) {
  val logger = KotlinLogging.logger {}

  internal inline fun <T : Message, reified S : Envelope<out T>> CoroutineScope.startConsumer(
      crossinline executor: suspend (T) -> Unit,
      topic: String,
      subscriptionName: String,
      subscriptionType: SubscriptionType,
      consumerName: String,
      concurrency: Int,
      topicDLQ: String?
  ) {
    when (subscriptionType) {
      SubscriptionType.Key_Shared ->
          repeat(concurrency) {
            // For Key_Shared subscription, we need to create a new consumer for each executor
            // coroutine
            launch {
              @Suppress("UNCHECKED_CAST")
              val consumer =
                  createConsumer<T, S>(
                      topic = topic,
                      subscriptionName =
                          subscriptionName, // subscriptionName MUST be the same for all concurrent
                      // instances!
                      subscriptionType = subscriptionType,
                      consumerName = "$consumerName-$it",
                      topicDLQ = topicDLQ,
                      config)
                      as Consumer<S>

              while (isActive) {
                // await() ensures this coroutine exits in case of throwable not caught
                val pulsarMessage =
                    try {
                      consumer.receiveAsync().await()
                    } catch (e: CancellationException) {
                      // if coroutine is canceled, we just exit the while loop
                      break
                    }

                val message =
                    try {
                      pulsarMessage.value.message()
                    } catch (e: Exception) {
                      logger.warn(e) {
                        "${pulsarMessage.messageId}: Exception when deserializing $pulsarMessage"
                      }
                      negativeAcknowledge(consumer, pulsarMessage.messageId)
                      continue
                    }

                logger.debug {
                  "Receiving consumerName='$consumerName-$it' messageId='${pulsarMessage.messageId}' key='${pulsarMessage.key}' message='$message'"
                }

                try {
                  executor(message)
                  consumer.acknowledge(pulsarMessage.messageId)
                } catch (e: Exception) {
                  logger.warn(e) { "${pulsarMessage.messageId}: Exception when handling $message" }
                  negativeAcknowledge(consumer, pulsarMessage.messageId)
                  continue
                }
              }
            }
          }
      else -> {
        // For other subscription, we can use the same consumer for all executor coroutines
        val consumer =
            createConsumer<T, S>(
                topic = topic,
                subscriptionName = subscriptionName,
                subscriptionType = subscriptionType,
                consumerName = consumerName,
                topicDLQ = topicDLQ,
                config)

        val channel = Channel<PulsarMessage<out Envelope<T>>>()

        // Channel is backpressure aware, so we can use it to send messages to the executor
        // coroutines
        launch {
          while (isActive) {
            // await() ensures this coroutine exits in case of throwable not caught
            try {
              channel.send(consumer.receiveAsync().await())
            } catch (e: CancellationException) {
              // if coroutine is canceled, we just exit the while loop
              break
            }
          }
        }

        repeat(concurrency) {
          launch {
            for (pulsarMessage in channel) {
              val message =
                  try {
                    pulsarMessage.value.message()
                  } catch (e: Exception) {
                    logger.warn(e) {
                      "${pulsarMessage.messageId}: Exception when deserializing $pulsarMessage"
                    }
                    negativeAcknowledge(consumer, pulsarMessage.messageId)
                    continue
                  }

              try {
                executor(message)
                consumer.acknowledge(pulsarMessage.messageId)
              } catch (e: Exception) {
                logger.warn(e) { "${pulsarMessage.messageId}: Exception when handling $message" }
                negativeAcknowledge(consumer, pulsarMessage.messageId)
                continue
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
      config: ConsumerConfig
  ): Consumer<out Envelope<T>> {
    logger.debug {
      "Creating Consumer consumerName='$consumerName' subscriptionName='$subscriptionName' subscriptionType='$subscriptionType' topic='$topic'"
    }

    val schema = Schema.AVRO(schemaDefinition<S>())

    return client
        .newConsumer(schema)
        .topic(topic)
        .subscriptionType(subscriptionType)
        .subscriptionName(subscriptionName)
        .consumerName(consumerName)
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
        .also { c ->
          config.loadConf?.also {
            logger.info { "subscription $subscriptionName: loadConf=$it" }
            c.loadConf(it)
          }
          config.subscriptionProperties?.also {
            logger.info { "subscription $subscriptionName: subscriptionProperties=$it" }
            c.subscriptionProperties(it)
          }
          config.ackTimeoutSeconds?.also {
            logger.info { "subscription $subscriptionName: ackTimeout=$it" }
            c.ackTimeout((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          config.isAckReceiptEnabled?.also {
            logger.info { "subscription $subscriptionName: isAckReceiptEnabled=$it" }
            c.isAckReceiptEnabled(it)
          }
          config.ackTimeoutTickTimeSeconds?.also {
            logger.info { "subscription $subscriptionName: ackTimeoutTickTime=$it" }
            c.ackTimeoutTickTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          config.negativeAckRedeliveryDelaySeconds?.also {
            logger.info { "subscription $subscriptionName: negativeAckRedeliveryDelay=$it" }
            c.negativeAckRedeliveryDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          config.defaultCryptoKeyReader?.also {
            logger.info { "subscription $subscriptionName: defaultCryptoKeyReader=$it" }
            c.defaultCryptoKeyReader(it)
          }
          config.cryptoFailureAction?.also {
            logger.info { "subscription $subscriptionName: cryptoFailureAction=$it" }
            c.cryptoFailureAction(it)
          }
          config.receiverQueueSize?.also {
            logger.info { "subscription $subscriptionName: receiverQueueSize=$it" }
            c.receiverQueueSize(it)
          }
          config.acknowledgmentGroupTimeSeconds?.also {
            logger.info { "subscription $subscriptionName: acknowledgmentGroupTime=$it" }
            c.acknowledgmentGroupTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          config.replicateSubscriptionState?.also {
            logger.info { "subscription $subscriptionName: replicateSubscriptionState=$it" }
            c.replicateSubscriptionState(it)
          }
          config.maxTotalReceiverQueueSizeAcrossPartitions?.also {
            logger.info {
              "subscription $subscriptionName: maxTotalReceiverQueueSizeAcrossPartitions=$it"
            }
            c.maxTotalReceiverQueueSizeAcrossPartitions(it)
          }
          config.priorityLevel?.also {
            logger.info { "subscription $subscriptionName: priorityLevel=$it" }
            c.priorityLevel(it)
          }
          config.properties?.also {
            logger.info { "subscription $subscriptionName: properties=$it" }
            c.properties(it)
          }
          config.autoUpdatePartitions?.also {
            logger.info { "subscription $subscriptionName: autoUpdatePartitions=$it" }
            c.autoUpdatePartitions(it)
          }
          config.autoUpdatePartitionsIntervalSeconds?.also {
            logger.info { "subscription $subscriptionName: autoUpdatePartitionsInterval=$it" }
            c.autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
          }
          config.enableBatchIndexAcknowledgment?.also {
            logger.info { "subscription $subscriptionName: enableBatchIndexAcknowledgment=$it" }
            c.enableBatchIndexAcknowledgment(it)
          }
          config.maxPendingChunkedMessage?.also {
            logger.info { "subscription $subscriptionName: maxPendingChunkedMessage=$it" }
            c.maxPendingChunkedMessage(it)
          }
          config.autoAckOldestChunkedMessageOnQueueFull?.also {
            logger.info {
              "subscription $subscriptionName: autoAckOldestChunkedMessageOnQueueFull=$it"
            }
            c.autoAckOldestChunkedMessageOnQueueFull(it)
          }
          config.expireTimeOfIncompleteChunkedMessageSeconds?.also {
            logger.info {
              "subscription $subscriptionName: expireTimeOfIncompleteChunkedMessage=$it"
            }
            c.expireTimeOfIncompleteChunkedMessage((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          config.startPaused?.also {
            logger.info { "subscription $subscriptionName: startPaused=$it" }
            c.startPaused(it)
          }
          // Dead Letter Queue
          topicDLQ?.also {
            when (subscriptionType) {
              SubscriptionType.Key_Shared,
              SubscriptionType.Shared -> {
                logger.info {
                  "subscription $subscriptionName: maxRedeliverCount=${config.maxRedeliverCount}"
                }
                c.deadLetterPolicy(
                    DeadLetterPolicy.builder()
                        .maxRedeliverCount(config.maxRedeliverCount)
                        .deadLetterTopic(it)
                        .build())
              }
              else -> Unit
            }
          }
        }
        .subscribe() as Consumer<out Envelope<T>>
  }
}
