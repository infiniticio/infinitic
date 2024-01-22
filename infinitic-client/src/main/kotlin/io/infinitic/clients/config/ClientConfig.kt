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
package io.infinitic.clients.config

import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.transport.config.Transport

data class ClientConfig @JvmOverloads constructor(
  /** Client name */
  override val name: String? = null,

  /** Transport configuration */
  override val transport: Transport = Transport.pulsar,

  /** Pulsar configuration */
  override val pulsar: Pulsar? = null,

  /** Shutdown Grace Period */
  override val shutdownGracePeriodInSeconds: Double = 10.0
) : ClientConfigInterface {

  companion object {
    /** Create ClientConfig from file in file system */
    @JvmStatic
    fun fromFile(vararg files: String): ClientConfig =
        loadConfigFromFile<ClientConfig>(*files)

    /** Create ClientConfig from file in resources directory */
    @JvmStatic
    fun fromResource(vararg resources: String): ClientConfig =
        loadConfigFromResource<ClientConfig>(*resources)

    /** Create ClientConfig from yaml strings */
    @JvmStatic
    fun fromYaml(vararg yamls: String): ClientConfig =
        loadConfigFromYaml<ClientConfig>(*yamls)
  }
}
