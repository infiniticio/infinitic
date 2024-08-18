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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.acceptDelayed
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticProducerAsync(
  private val mainChannels: InMemoryChannels,
  private val eventListenerChannels: InMemoryChannels,
  private val eventLoggerChannels: InMemoryChannels
) : InfiniticProducerAsync {

  private val logger = KotlinLogging.logger {}

  override var producerName = DEFAULT_NAME

  private fun <S : Message> Topic<S>.channelsForMessage(message: S): List<Channel<S>> {
    val entity = message.entity()

    return listOf(
        with(mainChannels) { channel(entity) },
        with(eventListenerChannels) { channel(entity) },
        with(eventLoggerChannels) { channel(entity) },
    )
  }

  private fun <S : Message> Topic<S>.channelsForDelayedMessage(message: S): List<Channel<DelayedMessage<S>>> {
    val entity = message.entity()

    return listOf(
        with(mainChannels) { channelForDelayed(entity) },
        with(eventListenerChannels) { channelForDelayed(entity) },
        with(eventLoggerChannels) { channelForDelayed(entity) },
    )
  }

  override suspend fun <T : Message> internalSendToAsync(
    message: T,
    topic: Topic<T>,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    when (topic.acceptDelayed) {
      true -> {
        topic.channelsForDelayedMessage(message).forEach {
          logger.trace { "Topic $topic(${it.id}): sending $message" }
          it.send(DelayedMessage(message, after))
          logger.debug { "Topic $topic(${it.id}): sent $message" }
        }
      }

      false -> {
        topic.channelsForMessage(message).forEach {
          logger.trace { "Topic $topic(${it.id}): sending $message" }
          it.send(message)
          logger.debug { "Topic $topic(${it.id}): sent $message" }
        }
      }
    }

    return CompletableFuture.completedFuture(Unit)
  }

  companion object {
    private const val DEFAULT_NAME = "inMemory"
  }
}


