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
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.acceptDelayed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class InMemoryInfiniticConsumerAsync(
  private val mainChannels: InMemoryChannels,
  private val eventListenerChannels: InMemoryChannels,
) : InfiniticConsumerAsync {

  override var logName: String? = null

  // Coroutine scope used to receive messages
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  override fun join() {
    runBlocking { consumingScope.coroutineContext.job.children.forEach { it.join() } }
  }

  private val logger = KotlinLogging.logger(logName ?: this::class.java.name)

  override fun close() {
    consumingScope.cancel()
    join()
  }

  override suspend fun <S : Message> start(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    concurrency: Int
  ) {
    val c = when (subscription.withKey) {
      true -> 1
      false -> concurrency
    }
    when (subscription.topic.acceptDelayed) {
      true -> {
        val channel = subscription.getDelayedChannel(entity)
        logger.info { "$subscription (${channel.id}) Starting consumer for $entity with concurrency = $c" }
        startLoopForDelayed(subscription, handler, beforeDlq, channel, c)
      }

      false -> {
        val channel = subscription.getChannel(entity)
        logger.info { "$subscription (${channel.id}) Starting consumer for $entity with concurrency = $c" }
        startLoop(subscription, handler, beforeDlq, channel, c)
      }
    }
  }

  // start an executor on a channel containing messages
  private suspend fun <S : Message> startLoop(
    subscription: Subscription<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    channel: Channel<S>,
    concurrency: Int
  ) = coroutineScope {
    repeat(concurrency) {
      launch(consumingScope.coroutineContext) {
        try {
          for (message in channel) {
            try {
              logger.trace { "$subscription (${channel.id})}: Handling $message" }
              handler(message, MillisInstant.now())
              logger.debug { "$subscription (${channel.id}): Handled $message" }
            } catch (e: Exception) {
              logger.warn(e) { "$subscription (${channel.id}): Error while processing message $message" }
              sendToDlq(beforeDlq, channel, message, e)
            }
          }
        } catch (e: CancellationException) {
          logger.info { "$subscription (${channel.id})} Canceled" }
        }
      }
    }
  }

  // start an executor on a channel containing delayed messages
  private suspend fun <S : Message> startLoopForDelayed(
    subscription: Subscription<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    channel: Channel<DelayedMessage<S>>,
    concurrency: Int
  ) = coroutineScope {
    repeat(concurrency) {
      launch(consumingScope.coroutineContext) {
        try {
          for (delayedMessage in channel) {
            try {
              val ts = MillisInstant.now()
              delay(delayedMessage.after.long)
              logger.trace { "$subscription (${channel.id}): Handling ${delayedMessage.message}" }
              handler(delayedMessage.message, ts)
              logger.debug { "$subscription (${channel.id}): Handled ${delayedMessage.message}" }
            } catch (e: Exception) {
              logger.warn(e) { "$subscription (${channel.id}): Error while processing delayed message ${delayedMessage.message}" }
              sendToDlq(beforeDlq, channel, delayedMessage.message, e)
            }
          }
        } catch (e: CancellationException) {
          logger.info { "$subscription (${channel.id})} Canceled" }
        }
      }
    }
  }

  private fun <S : Message> Subscription<S>.getDelayedChannel(entity: String) = when (this) {
    is MainSubscription -> with(mainChannels) { topic.channelForDelayed(entity) }
    is EventListenerSubscription -> with(eventListenerChannels) { topic.channelForDelayed(entity) }
  }

  private fun <S : Message> Subscription<S>.getChannel(entity: String) = when (this) {
    is MainSubscription -> with(mainChannels) { topic.channel(entity) }
    is EventListenerSubscription -> with(eventListenerChannels) { topic.channel(entity) }
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

