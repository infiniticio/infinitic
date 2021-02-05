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
import io.infinitic.pulsar.config.loadConfigFromFile
import io.infinitic.pulsar.config.loadConfigFromResource
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
        @JvmStatic
        fun from(
            name: String?,
            pulsarClient: PulsarClient,
            tenant: String,
            namespace: String
        ): InfiniticClient {
            // checks unicity if not null, provides a unique name if null
            val clientName = getPulsarName(pulsarClient, name)

            val clientOutput = PulsarOutputs.from(pulsarClient, tenant, namespace, clientName).clientOutput

            val clientResponseConsumer = PulsarConsumerFactory(pulsarClient, tenant, namespace)
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
                config.name,
                pulsarClient,
                config.pulsar.tenant,
                config.pulsar.namespace
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
