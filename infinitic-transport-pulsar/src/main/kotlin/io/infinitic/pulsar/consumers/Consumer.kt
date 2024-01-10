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
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.pulsar.client.PulsarInfiniticClient
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import kotlin.reflect.KClass
import org.apache.pulsar.client.api.Message as PulsarMessage

class Consumer(
  val client: PulsarInfiniticClient,
  private val consumerConfig: ConsumerConfig
) {

  val logger = KotlinLogging.logger {}

  internal suspend fun <T : Message, S : Envelope<out T>> runConsumer(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    schemaClass: KClass<S>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
    concurrency: Int
  ) = coroutineScope {
    logger.debug { "Starting $concurrency consumers on topic $topic with subscription $subscriptionName" }

    when (subscriptionType) {
      SubscriptionType.Key_Shared ->
        repeat(concurrency) {
          launch {
            val consumerNameIt = "$consumerName-$it"
            // For Key_Shared subscription, we must create a new consumer for each executor coroutine
            val consumer = getConsumer(
                schemaClass = schemaClass,
                topic = topic,
                topicDlq = topicDlq,
                subscriptionName = subscriptionName,
                subscriptionNameDlq = subscriptionNameDlq,
                subscriptionType = subscriptionType,
                consumerName = consumerNameIt,
            ).getOrThrow()

            while (isActive) {
              try {
                // await() is a suspendable and should be used instead of get()
                val pulsarMessage = consumer.receiveAsync().await()
                // process pulsar message
                processPulsarMessage(consumer, handler, beforeDlq, topic, pulsarMessage!!)
              } catch (e: CancellationException) {
                // exit while loop when coroutine is canceled
                break
              } catch (e: Exception) {
                // for other exception, we continue
                logWarn(e, topic, null) { "Exception in $consumerNameIt:" }
                continue
              }
            }
            logger.debug { "Closing consumer $consumerNameIt after cancellation" }
            client.closeConsumer(consumer)
            logger.info { "Closed consumer $consumerNameIt after cancellation" }
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

        // Channel is backpressure aware
        // we can use it to send messages to the executor coroutines
        val channel = Channel<PulsarMessage<S>>()

        // start executor coroutines
        val jobs = List(concurrency) {
          launch {
            try {
              for (pulsarMessage: PulsarMessage<S> in channel) {
                processPulsarMessage(consumer, handler, beforeDlq, topic, pulsarMessage)
              }
            } catch (e: CancellationException) {
              // exiting
            }
          }
        }
        // start message receiver
        while (isActive) {
          try {
            // await() is a suspendable and should be used instead of get()
            val pulsarMessage = consumer.receiveAsync().await()
            channel.send(pulsarMessage)
          } catch (e: CancellationException) {
            // if coroutine is canceled, we just exit the while loop
            break
          } catch (e: Exception) {
            // for other exception, we continue
            logWarn(e, topic, null) { "Exception in $consumerName:" }
            continue
          }
        }
        logger.debug { "Closing consumer $consumerName after cancellation" }
        withContext(NonCancellable) { jobs.joinAll() }
        client.closeConsumer(consumer)
        logger.info { "Closed consumer $consumerName after cancellation" }
      }
    }
  }

  private fun <T : Message, S : Envelope<out T>> processPulsarMessage(
    consumer: Consumer<S>,
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    topic: String,
    pulsarMessage: PulsarMessage<S>
  ) {
    val messageId = pulsarMessage.messageId
    val publishTime = MillisInstant(pulsarMessage.publishTime)

    val message: T
    try {
      logTrace(topic, messageId) { "Deserializing pulsar message $pulsarMessage" }
      message = pulsarMessage.value.message()
      logDebug(topic, messageId) { "Deserialized pulsar message into $message" }
    } catch (e: Exception) {
      logWarn(e, topic, messageId) { "Exception when deserializing $pulsarMessage" }
      negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
      return
    }

    try {
      logTrace(topic, messageId) { "Processing $message" }
      handler(message, publishTime)
      logDebug(topic, messageId) { "Processed $message" }
    } catch (e: Exception) {
      logWarn(e, topic, messageId) { "Exception when processing $message" }
      e.throwError()
      negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
      return
    }

    try {
      logTrace(topic, messageId) { "Acknowledging $message" }
      consumer.acknowledge(messageId)
      logDebug(topic, messageId) { "Acknowledged $message" }
    } catch (e: Exception) {
      logWarn(e, topic, messageId) { "Exception when acknowledging $message" }
      negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
      return
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
  private fun <T : Message, S : Envelope<out T>> negativeAcknowledge(
    consumer: Consumer<S>,
    pulsarMessage: PulsarMessage<out S>,
    beforeDlq: ((T, Exception) -> Unit)?,
    message: T?,
    cause: Exception
  ): Result<Unit> {
    val messageId = pulsarMessage.messageId
    val topic = consumer.topic

    // before sending to DLQ, we apply beforeDlq if any
    if (pulsarMessage.redeliveryCount == consumerConfig.maxRedeliverCount && beforeDlq != null) {
      when (message) {
        null -> logError(cause, topic, messageId) { "Exception when applying DLQ handler" }

        else -> try {
          logTrace(topic, messageId) { "Processing DLQ handler for $message}" }
          beforeDlq(message, cause)
          logWarn(null, topic, messageId) { "Processed DLQ handler for $message}" }
        } catch (e: Exception) {
          logError(e, topic, messageId) { "Exception when processing DLQ handler for $message}" }
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

  private fun logWarn(e: Exception?, topic: String, messageId: MessageId?, txt: () -> String) {
    logger.warn(e) { "Topic: $topic ${messageId?.let { "($messageId)" } ?: ""} - ${txt()}" }
  }

  private fun logError(e: Exception, topic: String, messageId: MessageId, txt: () -> String) {
    logger.error(e) { "Topic: $topic ($messageId) - ${txt()}" }
  }

  /**
   *  rethrowError is used to determine whether the cause of the CompletionException
   *  is an Error rather than an Exception and, if so, to throw that Error.
   */
  private fun Exception.throwError() {
    val e = if (this is CompletionException) cause else this
    if (e != null && e !is Exception) throw e
  }
}
