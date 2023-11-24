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
  val consumerConfig: ConsumerConfig
) : Namer(pulsarClient) {

  val logger = KotlinLogging.logger {}

  internal inline fun <T : Message, reified S : Envelope<out T>> CoroutineScope.startConsumer(
    crossinline handler: suspend (T) -> Unit,
    noinline beforeDlq: (suspend (T, Exception) -> Unit)?,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
    concurrency: Int
  ) {
    logger.debug { "Starting $concurrency consumers on topic $topic with subscription $subscriptionName" }

    when (subscriptionType) {
      SubscriptionType.Key_Shared ->
        repeat(concurrency) {
          // For Key_Shared subscription, we must create a new consumer for each executor coroutine
          launch {
            val consumer = createConsumer<T, S>(
                topic = topic,
                topicDlq = topicDlq,
                subscriptionName = subscriptionName, //  MUST be the same for all instances!
                subscriptionNameDlq = subscriptionNameDlq, //  MUST be the same for all instances!
                subscriptionType = subscriptionType,
                consumerName = "$consumerName-$it",
            )

            while (isActive) {
              // await() ensures this coroutine exits in case of throwable not caught
              val pulsarMessage: PulsarMessage<S> = try {
                consumer.receiveAsync().await()
              } catch (e: CancellationException) {
                // exit while loop when coroutine is canceled
                break
              }

              val messageId = pulsarMessage.messageId

              val message = try {
                logTrace(topic, messageId, "Deserializing received $pulsarMessage")
                pulsarMessage.value.message()
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when deserializing $pulsarMessage")
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
                // continue while loop
                continue
              }

              try {
                logTrace(topic, messageId, "Running deserialized $message")
                handler(message)
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when running $message")
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
                continue
              }

              try {
                logTrace(topic, messageId, "Acknowledging $message")
                consumer.acknowledge(messageId)
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when acknowledging $message")
                // the message will eventually time out and be redelivered
                continue
              }
            }
          }
        }

      else -> {
        // For other subscription, we can use the same consumer for all executor coroutines
        val consumer = createConsumer<T, S>(
            topic = topic,
            topicDlq = topicDlq,
            subscriptionName = subscriptionName,
            subscriptionNameDlq = subscriptionNameDlq,
            subscriptionType = subscriptionType,
            consumerName = consumerName,
        )

        val channel = Channel<PulsarMessage<S>>()

        // Channel is backpressure aware
        // we can use it to send messages to the executor coroutines
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
            for (pulsarMessage: PulsarMessage<S> in channel) {
              val messageId = pulsarMessage.messageId

              val message = try {
                logTrace(topic, messageId, "Deserializing received message $pulsarMessage")
                pulsarMessage.value.message()
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when deserializing $pulsarMessage")
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
                continue
              }

              try {
                logTrace(topic, messageId, "Running deserialized $message")
                handler(message)
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when running $message")
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
                continue
              }

              try {
                logTrace(topic, messageId, "Acknowledging $message")
                consumer.acknowledge(messageId)
              } catch (e: Exception) {
                logWarn(e, topic, messageId, "Exception when acknowledging $message")
                // the message will eventually time out and be redelivered
                continue
              }
            }
          }
        }
      }
    }
  }

  // if message has been redelivered too many times, send it to DLQ and tell Workflow Engine about that
  private suspend inline fun <T : Message, S : Envelope<out T>> negativeAcknowledge(
    consumer: Consumer<S>,
    pulsarMessage: PulsarMessage<out S>,
    noinline beforeDlq: (suspend (T, Exception) -> Unit)?,
    message: T?,
    cause: Exception
  ) {
    val messageId = pulsarMessage.messageId
    val topic = consumer.topic

    // before sending to DLQ, we tell Workflow Engine about that
    if (pulsarMessage.redeliveryCount == consumerConfig.maxRedeliverCount && beforeDlq != null) {
      when (message) {
        null -> logError(cause, topic, messageId, "Unable to tell that a message is sent to DLQ")

        else -> try {
          logTrace(topic, messageId, "Telling that a message is sent to DLQ $message}")
          beforeDlq(message, cause)
        } catch (e: Exception) {
          logError(e, topic, messageId, "Unable to tell that a message is sent to DLQ $message}")
        }
      }
    }

    try {
      consumer.negativeAcknowledge(pulsarMessage.messageId)
    } catch (e: Exception) {
      logError(e, topic, messageId, "Exception when negativeAcknowledging ${message ?: ""}")
    }
  }

  inline fun <reified S : Envelope<out Message>> getSchema(): Schema<S> =
      Schema.AVRO(schemaDefinition<S>())

  inline fun <T : Message, reified S : Envelope<out T>> createConsumer(
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String
  ): Consumer<S> {
    logger.debug {
      "Creating Consumer on topic='$topic', consumerName='$consumerName', subscriptionName='$subscriptionName', subscriptionType='$subscriptionType'"
    }

    // Get schema from envelope
    val schema: Schema<S> = getSchema<S>()

    return pulsarClient
        .newConsumer(schema)
        .topic(topic)
        .subscriptionType(subscriptionType)
        .subscriptionName(subscriptionName)
        .consumerName(consumerName)
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
        .also { c ->
          // Dead Letter Queue
          topicDlq?.also {
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
                        .build(),
                )
                // to avoid deletion of messages in DLQ, we create a subscription
                pulsarClient
                    .newConsumer(schema)
                    .topic(topicDlq)
                    .subscriptionType(SubscriptionType.Failover)
                    .subscriptionName(subscriptionNameDlq)
                    .consumerName("$consumerName-dlq")
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .subscribe()
                    // close the consumer immediately as we do not need it
                    .close()

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
        .subscribe() as Consumer<S>
  }

  private fun logTrace(topic: String, messageId: MessageId, message: String) {
    logger.trace { "Topic: $topic ($messageId) - $message" }
  }

  private fun logWarn(e: Exception, topic: String, messageId: MessageId, message: String) {
    logger.warn(e) { "Topic: $topic ($messageId) - $message" }
  }

  private fun logError(e: Exception, topic: String, messageId: MessageId, message: String) {
    logger.error(e) { "Topic: $topic ($messageId) - $message" }
  }
}
