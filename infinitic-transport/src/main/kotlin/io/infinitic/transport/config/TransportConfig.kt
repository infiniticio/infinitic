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
import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.inMemory.InMemoryChannels
import io.infinitic.inMemory.InMemoryInfiniticConsumerAsync
import io.infinitic.inMemory.InMemoryInfiniticProducerAsync
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync
import io.infinitic.pulsar.PulsarInfiniticProducerAsync
import io.infinitic.pulsar.client.PulsarInfiniticClient
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.ResourceManager

data class TransportConfig(
  /** Transport configuration */
  override val transport: Transport,

  /** Pulsar configuration */
  override val pulsar: Pulsar?,

  /** Shutdown Grace Period */
  override val shutdownGracePeriodInSeconds: Double
) : TransportConfigInterface {

  init {
    if (transport == Transport.pulsar) {
      require(pulsar != null) { "Missing Pulsar configuration" }
    }

    require(shutdownGracePeriodInSeconds >= 0) { "shutdownGracePeriodInSeconds must be >= 0" }
  }

  // we provide consumer and producer together,
  // as they must share the same configuration
  private val cp: Pair<InfiniticConsumerAsync, InfiniticProducerAsync> =
      when (transport) {
        Transport.pulsar -> with(ResourceManager.from(pulsar!!)) {
          val client = PulsarInfiniticClient(pulsar.client)
          val consumerAsync = PulsarInfiniticConsumerAsync(
              Consumer(client, pulsar.consumer),
              this,
              shutdownGracePeriodInSeconds,
          )
          val producerAsync = PulsarInfiniticProducerAsync(
              Producer(client, pulsar.producer),
              this,
          )

          // Pulsar client will be closed with consumer
          consumerAsync.addAutoCloseResource(pulsar.client)
          // Pulsar admin will be closed with consumer
          consumerAsync.addAutoCloseResource(pulsar.admin)

          Pair(consumerAsync, producerAsync)
        }

        Transport.inMemory -> {
          val channels = InMemoryChannels()
          val consumerAsync = InMemoryInfiniticConsumerAsync(channels)
          val producerAsync = InMemoryInfiniticProducerAsync(channels)

          // channels will be closed with consumer
          consumerAsync.addAutoCloseResource(channels)

          Pair(consumerAsync, producerAsync)
        }
      }

  /** Infinitic Consumer */
  val consumerAsync: InfiniticConsumerAsync = cp.first

  /** Infinitic Producer */
  val producerAsync: InfiniticProducerAsync = cp.second

  companion object {
    /** Create TransportConfig from file in file system */
    @JvmStatic
    fun fromFile(vararg files: String): TransportConfig =
        loadConfigFromFile(files.toList())

    /** Create TransportConfig from file in resources */
    @JvmStatic
    fun fromResource(vararg resources: String): TransportConfig =
        loadConfigFromResource(resources.toList())
  }
}

