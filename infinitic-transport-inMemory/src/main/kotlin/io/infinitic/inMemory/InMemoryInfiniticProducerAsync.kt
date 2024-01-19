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
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.isDelayed
import io.infinitic.common.transport.InfiniticProducerAsync
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticProducerAsync(private val channels: InMemoryChannels) :
  InfiniticProducerAsync {

  private val logger = KotlinLogging.logger {}

  override var producerName = DEFAULT_NAME

  private fun <S : Message> Topic<S>.channelForMessage(message: S): Channel<S> =
      with(channels) { channel(message.entity()) }

  private fun <S : Message> Topic<S>.channelForDelayedMessage(message: S): Channel<DelayedMessage<S>> =
      with(channels) { channelForDelayed(message.entity()) }

  override suspend fun <T : Message> internalSendToAsync(
    message: T,
    topic: Topic<T>,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    when (topic.isDelayed) {
      false -> {
        val channel = topic.channelForMessage(message)
        logger.debug { "Channel ${channel.id}: sending $message" }
        channel.send(message)
        logger.trace { "Channel ${channel.id}: sent" }
      }

      true -> {
        val channel = topic.channelForDelayedMessage(message)
        logger.debug { "Channel ${channel.id}: sending $message" }
        channel.send(DelayedMessage(message, after))
        logger.trace { "Channel ${channel.id}: sent" }
      }
    }

    return CompletableFuture.completedFuture(Unit)
  }

  companion object {
    private const val DEFAULT_NAME = "inMemory"
  }
}


