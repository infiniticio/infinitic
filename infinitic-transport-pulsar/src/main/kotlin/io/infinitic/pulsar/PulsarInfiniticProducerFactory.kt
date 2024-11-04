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
package io.infinitic.pulsar

import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.interfaces.InfiniticProducerFactory
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarProducerConfig
import io.infinitic.pulsar.resources.PulsarResources
import kotlinx.coroutines.runBlocking

class PulsarInfiniticProducerFactory(
  private val client: InfiniticPulsarClient,
  private val pulsarProducerConfig: PulsarProducerConfig,
  private val pulsarResources: PulsarResources
) : InfiniticProducerFactory {

  private var suggestedName: String? = null

  override suspend fun getName(): String {
    val namingTopic = with(pulsarResources) {
      NamingTopic.forEntity(null, init = true, checkConsumer = false)
    }
    // Get unique name
    return client.initName(namingTopic, suggestedName).getOrThrow()
  }

  override fun setName(name: String) {
    suggestedName = name
  }

  override fun getProducer(batchSendingConfig: BatchConfig?): PulsarInfiniticProducer {
    // init client name
    runBlocking { getName() }

    return PulsarInfiniticProducer(
        client,
        pulsarProducerConfig,
        pulsarResources,
        batchSendingConfig,
    )
  }
}
