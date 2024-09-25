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
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.acceptDelayed
import io.infinitic.common.transport.consumers.ConsumerSharedProcessor
import io.infinitic.common.transport.consumers.ConsumerUniqueProcessor
import io.infinitic.inMemory.channels.DelayedMessage
import io.infinitic.inMemory.channels.InMemoryChannels
import io.infinitic.inMemory.consumers.InMemoryTransportConsumer
import io.infinitic.inMemory.consumers.InMemoryTransportDelayedConsumer
import io.infinitic.inMemory.consumers.InMemoryTransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class InMemoryInfiniticConsumer(
  private val mainChannels: InMemoryChannels,
  private val eventListenerChannels: InMemoryChannels,
) : InfiniticConsumer {

  context(CoroutineScope)
  override suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int,
  ): Job {

    val loggedDeserialize: suspend (InMemoryTransportMessage<S>) -> S = { message ->
      logger.debug { "Deserializing message: ${message.messageId}" }
      message.toMessage().also {
        logger.trace { "Deserialized message: ${message.messageId}" }
      }
    }

    val loggedHandler: suspend (S, MillisInstant) -> Unit = { message, publishTime ->
      logger.debug { "Processing $message" }
      handler(message, publishTime)
      logger.trace { "Processed $message" }
    }

    fun buildConsumer(index: Int? = null): TransportConsumer<InMemoryTransportMessage<S>> {
      logger.debug { "Creating consumer ${index?.let { "${it + 1} " } ?: ""}on ${subscription.topic} for $entity " }
      return when (subscription.topic.acceptDelayed) {
        true -> InMemoryTransportDelayedConsumer(subscription.getChannelForDelayed(entity))
        false -> InMemoryTransportConsumer(subscription.getChannel(entity))
      }
    }

    return when (subscription.withKey) {
      true -> {
        // build the consumers synchronously (but in parallel)
        val consumers = coroutineScope {
          List(concurrency) { async { buildConsumer(it) } }.map { it.await() }
        }
        launch {
          coroutineScope {
            repeat(concurrency) {
              launch {
                ConsumerUniqueProcessor(consumers[it], loggedDeserialize, loggedHandler, null)
                    .start()
              }
            }
          }
        }
      }

      false -> {
        // build the consumer synchronously
        val consumer = buildConsumer()
        launch {
          ConsumerSharedProcessor(consumer, loggedDeserialize, loggedHandler, null)
              .start(concurrency)
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

