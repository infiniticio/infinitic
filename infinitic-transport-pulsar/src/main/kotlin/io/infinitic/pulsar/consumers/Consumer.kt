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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import org.apache.pulsar.client.api.Message as PulsarMessage

class Consumer(
  val client: PulsarInfiniticClient,
  private val consumerConfig: ConsumerConfig
) {

  val logger = KotlinLogging.logger {}

  private val consumingScope =
      CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

  val isActive get() = consumingScope.isActive

  fun cancel() {
    if (isActive) consumingScope.cancel()
  }

  fun join() = runBlocking {
    consumingScope.coroutineContext.job.children.forEach {
      try {
        it.join()
      } catch (e: CancellationException) {
        // do nothing
      }
    }
  }

  /**
   * Starts listening for messages on a Pulsar topic.
   *
   * Consumers are concurrently created on the current coroutine,
   * but the consuming loop is done on consumingScope.
   * The processing of message is done on a NonCancellable context
   * to guarantee that the ongoing messages are handled entirely during a shutdown
   *
   * @param T the type of the message
   * @param S the type of the envelope that wraps the message
   * @param handler the handler function to process the received message
   * @param beforeDlq the function to be called before sending a message to DLQ (Dead Letter Queue)
   * @param schema the schema used to deserialize the message envelope
   * @param topic the topic to listen to
   * @param topicDlq the DLQ topic to send failed messages to (can be null)
   * @param subscriptionName the subscription name for the regular topic
   * @param subscriptionNameDlq the subscription name for the DLQ topic
   * @param subscriptionType the subscription type to use (e.g., Exclusive, Shared, Key_Shared)
   * @param consumerName the name of the consumer
   * @param concurrency the number of consumers*/
  internal suspend fun <S : Message, T : Envelope<out S>> startListening(
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    schema: Schema<T>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    subscriptionInitialPosition: SubscriptionInitialPosition,
    consumerName: String,
    concurrency: Int
  ) = coroutineScope {

    logger.debug { "Starting $concurrency consumers on topic $topic with subscription $subscriptionName" }

    when (subscriptionType) {
      SubscriptionType.Key_Shared ->
        List(concurrency) {
          launch {
            val consumerNameIt = "$consumerName-$it"
            // For Key_Shared subscription, we must create a new consumer for each executor coroutine
            val consumer = getConsumer(
                schema = schema,
                topic = topic,
                topicDlq = topicDlq,
                subscriptionName = subscriptionName,
                subscriptionNameDlq = subscriptionNameDlq,
                subscriptionType = subscriptionType,
                subscriptionInitialPosition = subscriptionInitialPosition,
                consumerName = consumerNameIt,
            ).getOrThrow()

            launch(consumingScope.coroutineContext) {
              while (isActive) {
                try {
                  // await() is a suspendable and should be used instead of get()
                  val pulsarMessage = consumer.receiveAsync().await()
                  logDebug(topic, pulsarMessage.messageId) { "Received pulsar message" }
                  // this ensures that ongoing messages are processed
                  // even after scope is cancelled following an interruption or an Error
                  withContext(NonCancellable) {
                    processPulsarMessage(consumer, handler, beforeDlq, topic, pulsarMessage)
                  }
                } catch (e: CancellationException) {
                  // if current scope is canceled, we just exit the while loop
                  break
                } catch (e: Throwable) {
                  e.rethrowError(topic, where = "in $$consumerName")
                  continue
                }
              }
              closeConsumer(consumer)
            }
          }
        }

      else -> {
        // For other subscription, we can use the same consumer for all executor coroutines
        val consumer = getConsumer(
            schema = schema,
            topic = topic,
            topicDlq = topicDlq,
            subscriptionName = subscriptionName,
            subscriptionNameDlq = subscriptionNameDlq,
            subscriptionType = subscriptionType,
            subscriptionInitialPosition = subscriptionInitialPosition,
            consumerName = consumerName,
        ).getOrThrow()

        // Channel is backpressure aware
        // we can use it to send messages to the executor coroutines
        val channel = Channel<PulsarMessage<T>>()

        // start executor coroutines
        launch(consumingScope.coroutineContext) {
          val jobs = List(concurrency) {
            launch {
              try {
                for (pulsarMessage: PulsarMessage<T> in channel) {
                  // this ensures that ongoing messages are processed
                  // even after scope is cancelled following an interruption or an Error
                  withContext(NonCancellable) {
                    processPulsarMessage(consumer, handler, beforeDlq, topic, pulsarMessage)
                  }
                }
              } catch (e: CancellationException) {
                logDebug(topic) { "Processor #$it closed in $consumerName after cancellation" }
              }
            }
          }
          // start message receiver
          while (isActive) {
            try {
              // await() is a suspendable and should be used instead of get()
              val pulsarMessage = consumer.receiveAsync().await()
              logDebug(topic, pulsarMessage.messageId) { "Received pulsar message" }
              channel.send(pulsarMessage)
            } catch (e: CancellationException) {
              logDebug(topic) { "Exiting receiving loop in $consumerName" }
              // if current scope  is canceled, we just exit the while loop
              break
            } catch (e: Throwable) {
              e.rethrowError(topic, where = "in $$consumerName")
              continue
            }
          }
          logDebug(topic) { "Waiting completion of ongoing messages in $consumerName" }
          withContext(NonCancellable) { jobs.joinAll() }
          closeConsumer(consumer)
        }
      }
    }
  }

  private fun closeConsumer(consumer: Consumer<*>) {
    logger.debug { "Closing consumer ${consumer.consumerName} after cancellation" }
    client.closeConsumer(consumer)
        .onSuccess { logger.info { "Consumer ${consumer.consumerName} closed after cancellation" } }
        .onFailure { logger.warn(it) { "Unable to close consumer ${consumer.consumerName} after cancellation" } }
  }

  private suspend fun <T : Message, S : Envelope<out T>> processPulsarMessage(
    consumer: Consumer<S>,
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    topic: String,
    pulsarMessage: PulsarMessage<S>
  ) {
    try {
      val messageId = pulsarMessage.messageId
      val publishTime = MillisInstant(pulsarMessage.publishTime)

      val message: T
      try {
        logDebug(topic, messageId) { "Deserializing pulsar message $pulsarMessage" }
        message = pulsarMessage.value.message()
        logTrace(topic, messageId) { "Deserialized pulsar message into $message" }
      } catch (e: Exception) {
        logWarn(e, topic, messageId) { "Exception when deserializing $pulsarMessage" }
        negativeAcknowledge(consumer, pulsarMessage, beforeDlq, null, e)
        return
      }

      try {
        logDebug(topic, messageId) { "Processing $message" }
        handler(message, publishTime)
        logTrace(topic, messageId) { "Processed $message" }
      } catch (e: Exception) {
        logWarn(e, topic, messageId) { "Exception when processing $message" }
        negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
        return
      }

      try {
        logDebug(topic, messageId) { "Acknowledging $message" }
        consumer.acknowledge(messageId)
        logTrace(topic, messageId) { "Acknowledged $message" }
      } catch (e: Exception) {
        logWarn(e, topic, messageId) { "Exception when acknowledging $message" }
        negativeAcknowledge(consumer, pulsarMessage, beforeDlq, message, e)
        return
      }
    } catch (e: Throwable) {
      e.rethrowError(topic, "in processPulsarMessage")
    }
  }

  // if message has been redelivered too many times, send it to DLQ and tell Workflow Engine about that
  private suspend fun <T : Message, S : Envelope<out T>> negativeAcknowledge(
    consumer: Consumer<S>,
    pulsarMessage: PulsarMessage<out S>,
    beforeDlq: suspend (T?, Exception) -> Unit,
    message: T?,
    cause: Exception
  ): Result<Unit> {
    val messageId = pulsarMessage.messageId
    val topic = consumer.topic

    val msg = message?.let { "$it" } ?: "pulsar message ${pulsarMessage.messageId}"

    // before sending to DLQ, we apply beforeDlq if any
    if (pulsarMessage.redeliveryCount == consumerConfig.getMaxRedeliverCount()) {
      try {
        logDebug(topic, messageId) { "Processing DLQ handler for $msg}" }
        beforeDlq(message, cause)
        logTrace(topic, messageId) { "Processed DLQ handler for $msg}" }
      } catch (e: Exception) {
        logWarn(e, topic, messageId) { "Exception when processing DLQ handler for $msg}" }
      }
    }

    return try {
      consumer.negativeAcknowledge(pulsarMessage.messageId)
      Result.success(Unit)
    } catch (e: Exception) {
      logWarn(e, topic, messageId) { "Exception when negativeAcknowledging $msg}" }
      Result.failure(e)
    }
  }

  private fun Throwable.rethrowError(topic: String, where: String) {
    val e = if (this is CompletionException) (cause ?: this) else this
    when (e) {
      is Exception ->
        // Exceptions are only logged
        logWarn(e, topic) { "Exception $where" }

      else -> {
        // Other Throwable are rethrown and will kill the worker
        logError(e, topic) { "Error $where" }
        throw e
      }
    }
  }

  private fun <S : Envelope<out Message>> getConsumer(
    schema: Schema<S>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    subscriptionInitialPosition: SubscriptionInitialPosition,
    consumerName: String,
  ): Result<Consumer<S>> {
    val consumerDef = PulsarInfiniticClient.ConsumerDef(
        topic = topic,
        subscriptionName = subscriptionName, //  MUST be the same for all instances!
        subscriptionType = subscriptionType,
        subscriptionInitialPosition = subscriptionInitialPosition,
        consumerName = consumerName,
        consumerConfig = consumerConfig,
    )
    val consumerDefDlq = topicDlq?.let {
      PulsarInfiniticClient.ConsumerDef(
          topic = it,
          subscriptionName = subscriptionNameDlq, //  MUST be the same for all instances!
          subscriptionType = SubscriptionType.Shared,
          subscriptionInitialPosition = subscriptionInitialPosition,
          consumerName = "$consumerName-dlq",
          consumerConfig = consumerConfig,
      )
    }

    return client.newConsumer(schema, consumerDef, consumerDefDlq)
  }

  private fun logStr(topic: String, messageId: MessageId? = null, txt: () -> String) =
      "Topic: $topic ${messageId?.let { "($messageId)" } ?: ""} - ${txt()}"

  private fun logTrace(topic: String, messageId: MessageId? = null, txt: () -> String) {
    logger.trace { logStr(topic, messageId, txt) }
  }

  private fun logInfo(topic: String, messageId: MessageId? = null, txt: () -> String) {
    logger.trace { logStr(topic, messageId, txt) }
  }

  private fun logDebug(topic: String, messageId: MessageId? = null, txt: () -> String) {
    logger.debug { logStr(topic, messageId, txt) }
  }

  private fun logWarn(
    e: Exception?,
    topic: String,
    messageId: MessageId? = null,
    txt: () -> String
  ) {
    logger.warn(e) { logStr(topic, messageId, txt) }
  }

  private fun logError(
    e: Throwable?,
    topic: String,
    messageId: MessageId? = null,
    txt: () -> String
  ) {
    logger.error(e) { logStr(topic, messageId, txt) }
  }
}
