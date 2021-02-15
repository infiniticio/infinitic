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

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.messages.ClientResponseEnvelope
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.pulsar.config.loaders.loadConfigFromFile
import io.infinitic.pulsar.config.loaders.loadConfigFromResource
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutputs
import io.infinitic.pulsar.workers.startClientResponseWorker
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import kotlin.concurrent.thread
import io.infinitic.client.InfiniticClient as Client

@Suppress("unused")
class InfiniticClient private constructor(
    clientResponseConsumer: Consumer<ClientResponseEnvelope>,
    clientOutput: ClientOutput,
    private val closeFn: () -> Unit
) : Client(clientOutput) {

    companion object {
        /*
        Create InfiniticClient
        */
        @JvmStatic @JvmOverloads
        fun from(
            pulsarClient: PulsarClient,
            pulsarTenant: String,
            pulsarNamespace: String,
            clientName: String? = null
        ): InfiniticClient {
            // checks uniqueness if not null, provides a unique name if null
            val clientName = getPulsarName(pulsarClient, clientName)

            val clientOutput = PulsarOutputs.from(pulsarClient, pulsarTenant, pulsarNamespace, clientName).clientOutput

            val clientResponseConsumer = PulsarConsumerFactory(pulsarClient, pulsarTenant, pulsarNamespace)
                .newClientResponseConsumer(clientName)

            return InfiniticClient(clientResponseConsumer, clientOutput) { pulsarClient.close() }
        }

        /*
        Create InfiniticClient from a ClientConfig instance
        */
        @JvmStatic
        fun fromConfig(config: ClientConfig): InfiniticClient {
            val pulsarClient = PulsarClient
                .builder()
                .serviceUrl(config.pulsar.serviceUrl)
                .build()

            return from(
                pulsarClient,
                config.pulsar.tenant,
                config.pulsar.namespace,
                config.name
            )
        }

        /*
       Create InfiniticClient from a ClientConfig loaded from a resource
        */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
       Create InfiniticClient from a ClientConfig loaded from a file
        */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    init {
        val client = this
        thread {
            runBlocking {
                startClientResponseWorker(client, clientResponseConsumer)
            }
        }
    }

    fun close() = closeFn()
}
