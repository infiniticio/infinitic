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
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.inMemory.InMemoryChannels
import io.infinitic.inMemory.InMemoryInfiniticConsumerAsync
import io.infinitic.inMemory.InMemoryInfiniticProducerAsync
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync
import io.infinitic.pulsar.PulsarInfiniticProducerAsync
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
  private val cp: Pair<InfiniticConsumerAsync, InfiniticProducerAsync> =
      when (transport) {
        Transport.pulsar -> with(PulsarResources.from(pulsar!!)) {
          val client = PulsarInfiniticClient(pulsar.client)

          val consumerAsync = PulsarInfiniticConsumerAsync(
              Consumer(client, pulsar.consumer),
              this,
              shutdownGracePeriodInSeconds,
          )
          // Pulsar client and admin will be closed with consumer
          consumerAsync.addAutoCloseResource(pulsar.client)
          consumerAsync.addAutoCloseResource(pulsar.admin)

          val producerAsync = PulsarInfiniticProducerAsync(
              Producer(client, pulsar.producer),
              this,
          )

          Pair(consumerAsync, producerAsync)
        }

        Transport.inMemory -> {
          val mainChannels = InMemoryChannels()
          val eventListenerChannels = InMemoryChannels()
          val eventLoggerChannels = InMemoryChannels()
          val consumerAsync = InMemoryInfiniticConsumerAsync(mainChannels, eventListenerChannels, eventLoggerChannels)
          val producerAsync = InMemoryInfiniticProducerAsync(mainChannels, eventListenerChannels, eventLoggerChannels)
          Pair(consumerAsync, producerAsync)
        }
      }

  /** Infinitic Consumer */
  val consumerAsync: InfiniticConsumerAsync = cp.first

  /** Infinitic Producer */
  val producerAsync: InfiniticProducerAsync = cp.second
}

