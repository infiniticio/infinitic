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
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.hasKey
import io.infinitic.common.topics.isDelayed
import io.infinitic.common.transport.InfiniticConsumerAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class InMemoryInfiniticConsumerAsync(private val channels: InMemoryChannels) :
  InfiniticConsumerAsync {

  override var logName: String? = null

  override fun join() {
    //
  }

  private val logger = KotlinLogging.logger(logName ?: this::class.java.name)

  override fun close() {
    channels.close()
  }

  override suspend fun <S : Message> start(
    topic: Topic<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    entity: String,
    concurrency: Int
  ) {
    val c = when (topic.hasKey) {
      true -> 1
      false -> concurrency
    }
    when (topic.isDelayed) {
      true -> {
        val channel = with(channels) { topic.channelForDelayed(entity) }
        logger.info { "Channel ${channel.id}: Starting $topic consumer for $entity with concurrency = $c" }
        startLoopForDelayed(handler, beforeDlq, channel, c)
      }

      false -> {
        val channel = with(channels) { topic.channel(entity) }
        logger.info { "Channel ${channel.id}: Starting $topic consumer for $entity with concurrency = $c" }
        startLoop(handler, beforeDlq, channel, c)
      }
    }
  }

  // start an executor on a channel containing messages
  private suspend fun <T : Message> startLoop(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<T>,
    concurrency: Int = 1
  ) = repeat(concurrency) {
    channels.consume {
      try {
        for (message in channel) {
          try {
            logger.trace { "Channel ${channel.id}: Receiving $message" }
            handler(message, MillisInstant.now())
          } catch (e: Exception) {
            logger.warn(e) { "Channel ${channel.id}: Error while processing message $message" }
            sendToDlq(beforeDlq, channel, message, e)
          }
        }
      } catch (e: CancellationException) {
        logger.info { "Channel ${channel.id} Canceled" }
      }
    }
  }

  // start an executor on a channel containing delayed messages
  private suspend fun <T : Message> startLoopForDelayed(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<DelayedMessage<T>>,
    concurrency: Int = 1
  ) = repeat(concurrency) {
    channels.consume {
      try {
        for (delayedMessage in channel) {
          try {
            val ts = MillisInstant.now()
            delay(delayedMessage.after.long)
            logger.trace { "Channel ${channel.id}: Receiving ${delayedMessage.message}" }
            handler(delayedMessage.message, ts)
          } catch (e: Exception) {
            logger.warn(e) { "Channel ${channel.id}: Error while processing delayed message ${delayedMessage.message}" }
            sendToDlq(beforeDlq, channel, delayedMessage.message, e)
          }
        }
      } catch (e: CancellationException) {
        logger.info { "Channel ${channel.id} Canceled" }
      }
    }
  }

  // emulate sending to DLQ
  private suspend fun <T : Message> sendToDlq(
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<*>,
    message: T,
    e: Exception
  ) {
    try {
      logger.trace { "Channel ${channel.id}: Telling about message sent to DLQ $message}" }
      beforeDlq(message, e)
    } catch (e: Exception) {
      logger.error(e) { "Channel ${channel.id}: Unable to tell about message sent to DLQ $message" }
    }
  }
}

