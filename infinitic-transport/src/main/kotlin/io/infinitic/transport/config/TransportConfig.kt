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

import io.infinitic.common.transport.interfaces.InfiniticConsumerFactory
import io.infinitic.common.transport.interfaces.InfiniticProducerFactory
import io.infinitic.common.transport.interfaces.InfiniticResources

sealed class TransportConfig : AutoCloseable {
  /**
   * Specifies the duration, in seconds, allowed for the system to gracefully shut down.
   * During this period, the system will attempt to complete handle ongoing messages
   */
  abstract val shutdownGracePeriodSeconds: Double

  /**
   * This property denotes the origin of the CloudEvents being generated or consumed.
   */
  abstract val cloudEventSourcePrefix: String

  /**
   * This property provides methods to fetch available services and workflows,
   */
  abstract val resources: InfiniticResources

  /**
   * Provides methods to create consumers for processing messages.
   */
  abstract val consumerFactory: InfiniticConsumerFactory

  /**
   * Provides methods to create producers for sending messages.
   */
  abstract val producerFactory: InfiniticProducerFactory

  interface TransportConfigBuilder {
    fun build(): TransportConfig
  }
}
