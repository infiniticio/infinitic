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
package io.infinitic.inMemory

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.TransportMessage
import io.infinitic.common.transport.acceptDelayed
import io.infinitic.common.transport.consumers.ProcessorConsumer
import io.infinitic.inMemory.channels.DelayedMessage
import io.infinitic.inMemory.channels.InMemoryChannels
import io.infinitic.inMemory.consumers.InMemoryConsumer
import io.infinitic.inMemory.consumers.InMemoryDelayedConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class InMemoryInfiniticConsumer(
  private val mainChannels: InMemoryChannels,
  private val eventListenerChannels: InMemoryChannels,
) : InfiniticConsumer {

  override suspend fun <S : Message> buildConsumers(
    subscription: Subscription<S>,
    entity: String,
    occurrence: Int?
  ) = List(occurrence ?: 1) {
    logger.debug { "Creating consumer ${occurrence?.let { "${it + 1} " } ?: ""}on ${subscription.topic} for $entity " }
    when (subscription.topic.acceptDelayed) {
      true -> InMemoryDelayedConsumer(subscription.getChannelForDelayed(entity))
      false -> InMemoryConsumer(subscription.getChannel(entity))
    }
  }

  context(CoroutineScope)
  override suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    concurrency: Int,
    process: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    batchConfig: (suspend (S) -> BatchConfig?)?,
    batchProcess: (suspend (List<S>, List<MillisInstant>) -> Unit)?
  ): Job {

    val loggedDeserialize: suspend (TransportMessage<S>) -> S = { message ->
      logger.debug { "Deserializing message: ${message.messageId}" }
      message.deserialize().also {
        logger.trace { "Deserialized message: ${message.messageId}" }
      }
    }

    val loggedHandler: suspend (S, MillisInstant) -> Unit = { message, publishTime ->
      logger.debug { "Processing $message" }
      process(message, publishTime)
      logger.trace { "Processed $message" }
    }

    return when (subscription.withKey) {
      true -> {
        // build the consumers synchronously
        val consumers = buildConsumers(subscription, entity, concurrency)
        launch {
          repeat(concurrency) { index ->
            val processor = ProcessorConsumer(consumers[index], null)
            with(processor) { startAsync(1, loggedDeserialize, loggedHandler) }
          }
        }
      }

      false -> {
        // build the consumer synchronously
        val consumer = buildConsumer(subscription, entity)
        val processor = ProcessorConsumer(consumer, null)
        with(processor) {
          startAsync(
              concurrency,
              loggedDeserialize,
              loggedHandler,
              batchConfig,
              batchProcess,
          )
        }
      }
    }
  }

  private fun <S : Message> Subscription<S>.getChannelForDelayed(entity: String): Channel<DelayedMessage<S>> =
      when (this) {
        is MainSubscription -> with(mainChannels) { topic.channelForDelayed(entity) }
        is EventListenerSubscription -> with(eventListenerChannels) { topic.channelForDelayed(entity) }
      }

  private fun <S : Message> Subscription<S>.getChannel(entity: String): Channel<S> =
      when (this) {
        is MainSubscription -> with(mainChannels) { topic.channel(entity) }
        is EventListenerSubscription -> with(eventListenerChannels) { topic.channel(entity) }
      }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

