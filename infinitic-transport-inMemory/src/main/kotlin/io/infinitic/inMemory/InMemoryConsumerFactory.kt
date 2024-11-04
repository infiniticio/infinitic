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

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.interfaces.InfiniticConsumerFactory
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.inMemory.channels.InMemoryChannels
import io.infinitic.inMemory.consumers.InMemoryConsumer
import io.infinitic.inMemory.consumers.InMemoryTransportMessage
import kotlinx.coroutines.channels.Channel

class InMemoryConsumerFactory(
  private val mainChannels: InMemoryChannels,
  private val eventListenerChannels: InMemoryChannels,
) : InfiniticConsumerFactory {

  context(KLogger)
  override suspend fun <S : Message> buildConsumers(
    subscription: Subscription<S>,
    entity: String,
    batchConfig: BatchConfig?,
    occurrence: Int?
  ): List<TransportConsumer<InMemoryTransportMessage<S>>> = List(occurrence ?: 1) {
    InMemoryConsumer(subscription.topic, batchConfig, subscription.getChannel(entity))
  }

  context(KLogger)
  override suspend fun <S : Message> buildConsumer(
    subscription: Subscription<S>,
    entity: String,
    batchConfig: BatchConfig?,
  ): TransportConsumer<InMemoryTransportMessage<S>> =
      buildConsumers(subscription, entity, batchConfig, 1).first()

  private fun <S : Message> Subscription<S>.getChannel(entity: String): Channel<S> =
      when (this) {
        is MainSubscription -> with(mainChannels) { topic.channel(entity) }
        is EventListenerSubscription -> with(eventListenerChannels) { topic.channel(entity) }
      }

}

