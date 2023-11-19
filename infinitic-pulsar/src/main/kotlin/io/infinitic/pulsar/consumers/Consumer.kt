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
package io.infinitic.pulsar.consumers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.pulsar.namer.Namer
import io.infinitic.pulsar.schemas.schemaDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

class Consumer(
  override val pulsarClient: PulsarClient,
  private val consumerConfig: ConsumerConfig
) : Namer(pulsarClient) {

  val logger = KotlinLogging.logger {}

  internal inline fun <T : Message, reified S : Envelope<out T>> CoroutineScope.startConsumer(
    crossinline handler: suspend (T) -> Unit,
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
          // For Key_Shared subscription, we must create a new consumer for each executor coroutine
          launch {
            @Suppress("UNCHECKED_CAST")
            val consumer =
                createConsumer<T, S>(
                    topic = topic,
                    // subscriptionName MUST be the same for all concurrent instances!
                    subscriptionName = subscriptionName,
                    subscriptionType = subscriptionType,
                    consumerName = "$consumerName-$it",
                    topicDLQ = topicDLQ,
                    consumerConfig,
                )
                    as Consumer<S>

            while (isActive) {
              // await() ensures this coroutine exits in case of throwable not caught
              val pulsarMessage =
                  try {
                    consumer.receiveAsync().await()
                  } catch (e: CancellationException) {
                    // exit while loop when coroutine is canceled
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
                handler(message)
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
                consumerConfig,
            )

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
        // start executor coroutines
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
                handler(message)
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

  // Negative acknowledge a message. This will cause the message to be redelivered or send to DLQ
  private fun negativeAcknowledge(consumer: Consumer<*>, messageId: MessageId) {
    try {
      consumer.negativeAcknowledge(messageId)
    } catch (e: Exception) {
      logger.error(e) { "Exception when negativeAcknowledging $messageId" }
      // message will be timeout and be redelivered
    }
  }

  @Suppress("UNCHECKED_CAST")
  private inline fun <T : Message, reified S : Envelope<out T>> createConsumer(
    topic: String,
    subscriptionName: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
    topicDLQ: String?,
    consumerConfig: ConsumerConfig
  ): Consumer<out Envelope<T>> {
    logger.debug {
      "Creating Consumer consumerName='$consumerName' subscriptionName='$subscriptionName' subscriptionType='$subscriptionType' topic='$topic'"
    }

    val schema = Schema.AVRO(schemaDefinition<S>())

    return pulsarClient
        .newConsumer(schema)
        .topic(topic)
        .subscriptionType(subscriptionType)
        .subscriptionName(subscriptionName)
        .consumerName(consumerName)
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
        .also { c ->
          // Dead Letter Queue
          topicDLQ?.also {
            when (subscriptionType) {
              SubscriptionType.Key_Shared,
              SubscriptionType.Shared -> {
                logger.info {
                  "subscription $subscriptionName: maxRedeliverCount=${consumerConfig.maxRedeliverCount}"
                }
                c.deadLetterPolicy(
                    DeadLetterPolicy.builder()
                        .maxRedeliverCount(consumerConfig.maxRedeliverCount)
                        .deadLetterTopic(it)
                        .initialSubscriptionName("$subscriptionName-dlq")
                        .build(),
                )
                // remove default ackTimeout set by the deadLetterPolicy
                // https://github.com/apache/pulsar/issues/8484
                c.ackTimeout(0, TimeUnit.MILLISECONDS)
              }

              else -> Unit
            }
          }
          // must be set after deadLetterPolicy
          consumerConfig.ackTimeoutSeconds?.also {
            logger.info {
              "subscription $subscriptionName: ackTimeout=${consumerConfig.ackTimeoutSeconds}"
            }
            c.ackTimeout(
                (consumerConfig.ackTimeoutSeconds * 1000).toLong(),
                TimeUnit.MILLISECONDS,
            )
          }
          consumerConfig.loadConf?.also {
            logger.info { "subscription $subscriptionName: loadConf=$it" }
            c.loadConf(it)
          }
          consumerConfig.subscriptionProperties?.also {
            logger.info { "subscription $subscriptionName: subscriptionProperties=$it" }
            c.subscriptionProperties(it)
          }
          consumerConfig.isAckReceiptEnabled?.also {
            logger.info { "subscription $subscriptionName: isAckReceiptEnabled=$it" }
            c.isAckReceiptEnabled(it)
          }
          consumerConfig.ackTimeoutTickTimeSeconds?.also {
            logger.info { "subscription $subscriptionName: ackTimeoutTickTime=$it" }
            c.ackTimeoutTickTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          consumerConfig.negativeAckRedeliveryDelaySeconds?.also {
            logger.info { "subscription $subscriptionName: negativeAckRedeliveryDelay=$it" }
            c.negativeAckRedeliveryDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          consumerConfig.defaultCryptoKeyReader?.also {
            logger.info { "subscription $subscriptionName: defaultCryptoKeyReader=$it" }
            c.defaultCryptoKeyReader(it)
          }
          consumerConfig.cryptoFailureAction?.also {
            logger.info { "subscription $subscriptionName: cryptoFailureAction=$it" }
            c.cryptoFailureAction(it)
          }
          consumerConfig.receiverQueueSize?.also {
            logger.info { "subscription $subscriptionName: receiverQueueSize=$it" }
            c.receiverQueueSize(it)
          }
          consumerConfig.acknowledgmentGroupTimeSeconds?.also {
            logger.info { "subscription $subscriptionName: acknowledgmentGroupTime=$it" }
            c.acknowledgmentGroupTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          consumerConfig.replicateSubscriptionState?.also {
            logger.info { "subscription $subscriptionName: replicateSubscriptionState=$it" }
            c.replicateSubscriptionState(it)
          }
          consumerConfig.maxTotalReceiverQueueSizeAcrossPartitions?.also {
            logger.info {
              "subscription $subscriptionName: maxTotalReceiverQueueSizeAcrossPartitions=$it"
            }
            c.maxTotalReceiverQueueSizeAcrossPartitions(it)
          }
          consumerConfig.priorityLevel?.also {
            logger.info { "subscription $subscriptionName: priorityLevel=$it" }
            c.priorityLevel(it)
          }
          consumerConfig.properties?.also {
            logger.info { "subscription $subscriptionName: properties=$it" }
            c.properties(it)
          }
          consumerConfig.autoUpdatePartitions?.also {
            logger.info { "subscription $subscriptionName: autoUpdatePartitions=$it" }
            c.autoUpdatePartitions(it)
          }
          consumerConfig.autoUpdatePartitionsIntervalSeconds?.also {
            logger.info { "subscription $subscriptionName: autoUpdatePartitionsInterval=$it" }
            c.autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
          }
          consumerConfig.enableBatchIndexAcknowledgment?.also {
            logger.info { "subscription $subscriptionName: enableBatchIndexAcknowledgment=$it" }
            c.enableBatchIndexAcknowledgment(it)
          }
          consumerConfig.maxPendingChunkedMessage?.also {
            logger.info { "subscription $subscriptionName: maxPendingChunkedMessage=$it" }
            c.maxPendingChunkedMessage(it)
          }
          consumerConfig.autoAckOldestChunkedMessageOnQueueFull?.also {
            logger.info {
              "subscription $subscriptionName: autoAckOldestChunkedMessageOnQueueFull=$it"
            }
            c.autoAckOldestChunkedMessageOnQueueFull(it)
          }
          consumerConfig.expireTimeOfIncompleteChunkedMessageSeconds?.also {
            logger.info {
              "subscription $subscriptionName: expireTimeOfIncompleteChunkedMessage=$it"
            }
            c.expireTimeOfIncompleteChunkedMessage((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          consumerConfig.startPaused?.also {
            logger.info { "subscription $subscriptionName: startPaused=$it" }
            c.startPaused(it)
          }
        }
        .subscribe() as Consumer<out Envelope<T>>
  }
}
