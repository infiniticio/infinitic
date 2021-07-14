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

import io.infinitic.client.AbstractInfiniticClient
import io.infinitic.client.InfiniticClient
import io.infinitic.common.clients.data.ClientName
import io.infinitic.config.ClientConfig
import io.infinitic.config.data.Transport
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startClientResponseWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.apache.pulsar.client.api.PulsarClient
import io.infinitic.inMemory.InMemoryInfiniticClient as InMemoryClient

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class PulsarInfiniticClient @JvmOverloads constructor(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val pulsarTenant: String,
    @JvmField val pulsarNamespace: String,
    name: String? = null
) : AbstractInfiniticClient() {

    private var job: Job

    private val producerName = getProducerName(pulsarClient, name)

    override val scope = CoroutineScope(Dispatchers.IO + Job())

    override val clientName = ClientName(producerName)

    private val pulsarOutput =
        PulsarOutput.from(pulsarClient, pulsarTenant, pulsarNamespace, producerName)

    override val sendToTaskTagEngine =
        pulsarOutput.sendToTaskTagEngine(TopicType.NEW, true)

    override val sendToTaskEngine =
        pulsarOutput.sendToTaskEngine(TopicType.NEW, null, true)

    override val sendToWorkflowTagEngine =
        pulsarOutput.sendToWorkflowTagEngine(TopicType.NEW, true)

    override val sendToWorkflowEngine =
        pulsarOutput.sendToWorkflowEngine(TopicType.NEW, true)

    override fun close() {
        job.cancel()
        pulsarClient.close()
    }

    init {
        val clientResponseConsumer = PulsarConsumerFactory(pulsarClient, pulsarTenant, pulsarNamespace)
            .newClientConsumer(producerName, ClientName(producerName))

        job = scope.startClientResponseWorker(this, clientResponseConsumer)
    }

    companion object {

        /**
         * Create Client from a custom PulsarClient and a ClientConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, clientConfig: ClientConfig): InfiniticClient = when (clientConfig.transport) {
            Transport.pulsar -> PulsarInfiniticClient(
                pulsarClient,
                clientConfig.pulsar!!.tenant,
                clientConfig.pulsar!!.namespace,
                clientConfig.name
            )

            Transport.inMemory -> InMemoryClient.fromConfig(clientConfig)
        }

        /**
         * Create Client from a ClientConfig instance
         */
        @JvmStatic
        fun fromConfig(clientConfig: ClientConfig): InfiniticClient =
            from(clientConfig.pulsar!!.client, clientConfig)

        /**
         * Create Client from file in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(ClientConfig.fromResource(*resources))

        /**
         * Create Client from file in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(ClientConfig.fromFile(*files))
    }
}
