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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  val isActive get() = consumingScope.isActive

  fun cancel() {
    if (consumingScope.isActive) {
      consumingScope.cancel()
    }
  }

  fun join() = runBlocking {
    consumingScope.coroutineContext.job.children.forEach {
      try {
        it.join()
      } catch (e: CancellationException) {
        //
      }
    }
  }

  internal fun <T : Message, S : Envelope<out T>> runAsync(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    schemaClass: KClass<S>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
    concurrency: Int
  ) = consumingScope.future {
    logger.debug { "Starting $concurrency consumers on topic $topic with subscription $subscriptionName" }

    when (subscriptionType) {
      SubscriptionType.Key_Shared -> try {
        List(concurrency) {
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
        }.joinAll()
      } catch (e: CancellationException) {
        logInfo(topic) { "All consumers closed after cancellation" }
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
        logDebug(topic, null) { "Waiting completion of ongoing messages in $consumerName" }
        withContext(NonCancellable) { jobs.joinAll() }
        closeConsumer(consumer)
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
    if (pulsarMessage.redeliveryCount == consumerConfig.maxRedeliverCount) {
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
