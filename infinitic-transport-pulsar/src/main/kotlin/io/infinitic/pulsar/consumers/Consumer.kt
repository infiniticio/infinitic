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
import io.infinitic.pulsar.client.PulsarInfiniticClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.CancellationException
import kotlin.reflect.KClass
import org.apache.pulsar.client.api.Message as PulsarMessage

class Consumer(
  val client: PulsarInfiniticClient,
  val consumerConfig: ConsumerConfig
) {

  val logger = KotlinLogging.logger {}

  internal fun <T : Message, S : Envelope<out T>> CoroutineScope.startConsumer(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    schemaClass: KClass<S>,
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
          val consumer = getConsumer(
              schemaClass = schemaClass,
              topic = topic,
              topicDlq = topicDlq,
              subscriptionName = subscriptionName,
              subscriptionNameDlq = subscriptionNameDlq,
              subscriptionType = subscriptionType,
              consumerName = "$consumerName-$it",
          ).getOrThrow()

          launch {
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
                logTrace(topic, messageId) { "Deserializing received $pulsarMessage" }
                pulsarMessage.value.message()
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when deserializing $pulsarMessage" }
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
                // continue while loop
                continue
              }

              logDebug(topic, messageId) { "Received $message" }

              try {
                logTrace(topic, messageId) { "Running deserialized $message" }
                handler(message)
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when running $message" }
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
                continue
              }

              try {
                logTrace(topic, messageId) { "Acknowledging $message" }
                consumer.acknowledge(messageId)
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when acknowledging $message" }
                // the message will eventually time out and be redelivered
                continue
              }
            }
          }
        }

      else -> {
        // For other subscription, we can use the same consumer for all executor coroutines
        val consumer = getConsumer(
            schemaClass = schemaClass,
            topic = topic,
            topicDlq = topicDlq,
            subscriptionName = subscriptionName,
            subscriptionNameDlq = subscriptionNameDlq,
            subscriptionType = subscriptionType,
            consumerName = consumerName,
        ).getOrThrow()

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
                logTrace(topic, messageId) { "Deserializing received message $pulsarMessage" }
                pulsarMessage.value.message()
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when deserializing $pulsarMessage" }
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
                continue
              }

              try {
                logTrace(topic, messageId) { "Running deserialized $message" }
                handler(message)
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when running $message" }
                negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
                continue
              }

              try {
                logTrace(topic, messageId) { "Acknowledging $message" }
                consumer.acknowledge(messageId)
              } catch (e: Exception) {
                logWarn(e, topic, messageId) { "Exception when acknowledging $message" }
                // the message will eventually time out and be redelivered
                continue
              }
            }
          }
        }
      }
    }
  }

  private fun <S : Envelope<*>> getConsumer(
    schemaClass: KClass<S>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
  ): Result<Consumer<S>> {
    val consumerDef = PulsarInfiniticClient.ConsumerDef(
        topic = topic,
        subscriptionName = subscriptionName, //  MUST be the same for all instances!
        subscriptionType = subscriptionType,
        consumerName = consumerName,
        consumerConfig = consumerConfig,
    )
    val consumerDefDlq = topicDlq?.let {
      PulsarInfiniticClient.ConsumerDef(
          topic = it,
          subscriptionName = subscriptionNameDlq, //  MUST be the same for all instances!
          subscriptionType = SubscriptionType.Shared,
          consumerName = "$consumerName-dlq",
          consumerConfig = consumerConfig,
      )
    }

    return client.newConsumer(schemaClass, consumerDef, consumerDefDlq)
  }

  // if message has been redelivered too many times, send it to DLQ and tell Workflow Engine about that
  private suspend fun <T : Message, S : Envelope<out T>> negativeAcknowledge(
    consumer: Consumer<S>,
    pulsarMessage: PulsarMessage<out S>,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    message: T?,
    cause: Exception
  ): Result<Unit> {
    val messageId = pulsarMessage.messageId
    val topic = consumer.topic

    // before sending to DLQ, we tell Workflow Engine about that
    if (pulsarMessage.redeliveryCount == consumerConfig.maxRedeliverCount && beforeDlq != null) {
      when (message) {
        null -> logError(cause, topic, messageId) { "Unable to tell that a message is sent to DLQ" }

        else -> try {
          logTrace(topic, messageId) { "Telling that a message is sent to DLQ $message}" }
          beforeDlq(message, cause)
        } catch (e: Exception) {
          logError(e, topic, messageId) { "Unable to tell that a message is sent to DLQ $message}" }
        }
      }
    }

    return try {
      consumer.negativeAcknowledge(pulsarMessage.messageId)
      Result.success(Unit)
    } catch (e: Exception) {
      logError(e, topic, messageId) { "Exception when negativeAcknowledging ${message ?: ""}" }
      Result.failure(e)
    }
  }

  private fun logTrace(topic: String, messageId: MessageId, txt: () -> String) {
    logger.trace { "Topic: $topic ($messageId) - ${txt()}" }
  }

  private fun logDebug(topic: String, messageId: MessageId, txt: () -> String) {
    logger.debug { "Topic: $topic ($messageId) - ${txt()}" }
  }

  private fun logWarn(e: Exception, topic: String, messageId: MessageId, txt: () -> String) {
    logger.warn(e) { "Topic: $topic ($messageId) - ${txt()}" }
  }

  private fun logError(e: Exception, topic: String, messageId: MessageId, txt: () -> String) {
    logger.error(e) { "Topic: $topic ($messageId) - ${txt()}" }
  }
}
