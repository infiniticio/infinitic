/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar

import com.sksamuel.hoplite.ConfigLoader
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.pulsar.transport.PulsarOutputs
import org.apache.pulsar.client.api.PulsarClient
import io.infinitic.client.InfiniticClient as Client

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticClient(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val tenant: String,
    @JvmField val namespace: String
) : Client(PulsarOutputs.from(pulsarClient, tenant, namespace).clientOutput) {
    companion object {
        @JvmStatic
        fun fromConfigFile(configPath: String): InfiniticClient {
            // loaf Config instance
            val config: ClientConfig = ConfigLoader().loadConfigOrThrow(configPath)
            // build Pulsar client from config
            val pulsarClient: PulsarClient = PulsarClient.builder().serviceUrl(config.pulsar.serviceUrl).build()

            return InfiniticClient(pulsarClient, config.pulsar.tenant, config.pulsar.namespace)
        }
    }

    fun close() = pulsarClient.close()
}
