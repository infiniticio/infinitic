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
package io.infinitic.transport.config

import io.infinitic.autoclose.addAutoCloseResource
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.inMemory.InMemoryChannels
import io.infinitic.inMemory.InMemoryInfiniticConsumer
import io.infinitic.inMemory.InMemoryInfiniticProducer
import io.infinitic.pulsar.PulsarInfiniticConsumer
import io.infinitic.pulsar.PulsarInfiniticProducer
import io.infinitic.pulsar.client.PulsarInfiniticClient
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.ResourceManager

data class TransportConfigData(
  /** Transport configuration */
  override val transport: Transport = Transport.pulsar,

  /** Pulsar configuration */
  override val pulsar: Pulsar? = null
) : TransportConfig {

  init {
    if (transport == Transport.pulsar) {
      require(pulsar != null) { "Missing Pulsar configuration" }
    }
  }

  // we provide consumer and producer together,
  // as they must share the same configuration (InMemoryChannels instance)
  private val cp: Pair<InfiniticConsumer, InfiniticProducer> =
      when (transport) {
        Transport.pulsar -> with(ResourceManager.from(pulsar!!)) {
          val client = PulsarInfiniticClient(pulsar.client)
          val consumer = PulsarInfiniticConsumer(Consumer(client, pulsar.consumer), this)
          val producer = PulsarInfiniticProducer(Producer(client, pulsar.producer), this)

          // Pulsar client will be closed with consumer
          consumer.addAutoCloseResource(pulsar.client)
          // Pulsar admin will be closed with consumer
          consumer.addAutoCloseResource(pulsar.admin)

          Pair(consumer, producer)
        }

        Transport.inMemory -> {
          val channels = InMemoryChannels()
          val consumer = InMemoryInfiniticConsumer(channels)
          val producer = InMemoryInfiniticProducer(channels)

          // channels will be closed with consumer
          consumer.addAutoCloseResource(channels)

          Pair(consumer, producer)
        }
      }

  /** Infinitic Consumer */
  override val consumer = cp.first

  /** Infinitic Producer */
  override val producer = cp.second
}
