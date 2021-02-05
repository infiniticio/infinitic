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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.pulsar.config.loadConfigFromFile
import io.infinitic.pulsar.config.loadConfigFromResource
import io.infinitic.pulsar.transport.PulsarOutputs
import org.apache.pulsar.client.api.PulsarClient
import io.infinitic.client.InfiniticClient as Client

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticClient(
    @JvmField val name: String,
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val tenant: String,
    @JvmField val namespace: String,
    producerName: String? = null
) : Client(ClientName(name), PulsarOutputs.from(pulsarClient, tenant, namespace, producerName).clientOutput) {
    companion object {
        /*
        Create InfiniticClient from a ClientConfig
        */
        @JvmStatic
        fun fromConfig(config: ClientConfig): InfiniticClient {
            val pulsarClient: PulsarClient = PulsarClient
                .builder()
                .serviceUrl(config.pulsar.serviceUrl)
                .build()

            return InfiniticClient(
                config.name!!,
                pulsarClient,
                config.pulsar.tenant,
                config.pulsar.namespace,
                when (config.name) {
                    null -> null
                    else -> "client: ${config.name}"
                }
            )
        }

        /*
       Create InfiniticClient from a ClientConfig loaded from a resource
        */
        @JvmStatic
        fun fromResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
       Create InfiniticClient from a ClientConfig loaded from a file
        */
        @JvmStatic
        fun fromFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    fun close() = pulsarClient.close()
}
