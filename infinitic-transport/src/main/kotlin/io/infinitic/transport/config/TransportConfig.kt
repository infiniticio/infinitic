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
import io.infinitic.common.transport.InfiniticResources
import io.infinitic.inMemory.InMemoryChannels
import io.infinitic.inMemory.InMemoryInfiniticConsumer
import io.infinitic.inMemory.InMemoryInfiniticProducer
import io.infinitic.inMemory.InMemoryInfiniticResources
import io.infinitic.pulsar.PulsarInfiniticConsumer
import io.infinitic.pulsar.PulsarInfiniticProducer
import io.infinitic.pulsar.PulsarInfiniticResources
import io.infinitic.pulsar.client.PulsarInfiniticClient
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.PulsarResources
import java.net.URLEncoder

data class TransportConfig(
  /** Transport configuration */
  override val transport: Transport,

  /** Pulsar configuration */
  override val pulsar: PulsarConfig?,

  /** Shutdown Grace Period */
  override val shutdownGracePeriodInSeconds: Double
) : TransportConfigInterface {

  init {
    if (transport == Transport.pulsar) {
      require(pulsar != null) { "Missing Pulsar configuration" }
    }

    require(shutdownGracePeriodInSeconds >= 0) { "shutdownGracePeriodInSeconds must be >= 0" }
  }

  /** This is used as source prefix for CloudEvents */
  val source: String = when (transport) {
    Transport.pulsar -> pulsar!!.brokerServiceUrl.removeSuffix("/") + "/" +
        URLEncoder.encode(pulsar.tenant, Charsets.UTF_8) + "/" +
        URLEncoder.encode(pulsar.namespace, Charsets.UTF_8)

    Transport.inMemory -> "inmemory"
  }

  // we provide consumer and producer together,
  // as they must share the same configuration
  private val cp: Triple<InfiniticResources, InfiniticConsumer, InfiniticProducer> =
      when (transport) {
        Transport.pulsar -> with(PulsarResources.from(pulsar!!)) {
          val client = PulsarInfiniticClient(pulsar.client)

          val resources = PulsarInfiniticResources(this)

          val consumer = PulsarInfiniticConsumer(
              Consumer(client, pulsar.consumer),
              this,
              shutdownGracePeriodInSeconds,
          )
          // Pulsar client and admin will be closed with consumer
          consumer.addAutoCloseResource(pulsar.client)
          consumer.addAutoCloseResource(pulsar.admin)

          val producer = PulsarInfiniticProducer(
              Producer(client, pulsar.producer),
              this,
          )

          Triple(resources, consumer, producer)
        }

        Transport.inMemory -> {
          val mainChannels = InMemoryChannels()
          val eventListenerChannels = InMemoryChannels()
          val resources = InMemoryInfiniticResources(mainChannels)
          val consumer = InMemoryInfiniticConsumer(mainChannels, eventListenerChannels)
          val producer = InMemoryInfiniticProducer(mainChannels, eventListenerChannels)
          Triple(resources, consumer, producer)
        }
      }

  /** Infinitic Resources */
  val resources = cp.first

  /** Infinitic Consumer */
  val consumer = cp.second

  /** Infinitic Producer */
  val producer = cp.third
}

