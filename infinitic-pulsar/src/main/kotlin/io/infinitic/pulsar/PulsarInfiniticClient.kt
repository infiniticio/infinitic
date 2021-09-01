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

import io.infinitic.client.InfiniticClient
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startClientResponseWorker
import org.apache.pulsar.client.api.PulsarClient

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class PulsarInfiniticClient @JvmOverloads constructor(
    val pulsarClient: PulsarClient,
    val pulsarTenant: String,
    val pulsarNamespace: String,
    name: String? = null
) : InfiniticClient() {

    private val producerName by lazy { getProducerName(pulsarClient, pulsarTenant, pulsarNamespace, name) }

    override val clientName by lazy { ClientName(producerName) }

    private val pulsarOutput by lazy {
        // initialize response job handler
        val clientResponseConsumer = PulsarConsumerFactory(pulsarClient, pulsarTenant, pulsarNamespace)
            .newClientConsumer(producerName, ClientName(producerName))

        runningScope.startClientResponseWorker(this, clientResponseConsumer)

        // returns PulsarOutput instance
        PulsarOutput.from(pulsarClient, pulsarTenant, pulsarNamespace, producerName)
    }

    override val sendToTaskTagEngine by lazy {
        pulsarOutput.sendToTaskTagEngine(TopicType.NEW)
    }

    override val sendToTaskEngine by lazy {
        pulsarOutput.sendToTaskEngine(TopicType.NEW, null)
    }

    override val sendToWorkflowTagEngine by lazy {
        pulsarOutput.sendToWorkflowTagEngine(TopicType.NEW)
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine by lazy {
        pulsarOutput.sendToWorkflowEngine(TopicType.NEW)
    }

    override fun close() {
        super.close()
        pulsarClient.close()
    }

    companion object {
        /**
         * Create PulsarInfiniticClient from a custom PulsarClient and a ClientConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, clientConfig: ClientConfig) = PulsarInfiniticClient(
            pulsarClient,
            clientConfig.pulsar!!.tenant,
            clientConfig.pulsar.namespace,
            clientConfig.name
        )

        /**
         * Create PulsarInfiniticClient from a ClientConfig instance
         */
        @JvmStatic
        fun fromConfig(clientConfig: ClientConfig): InfiniticClient = from(clientConfig.pulsar!!.client, clientConfig)

        /**
         * Create PulsarInfiniticClient from file in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) = fromConfig(ClientConfig.fromResource(*resources))

        /**
         * Create PulsarInfiniticClient from file in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) = fromConfig(ClientConfig.fromFile(*files))
    }
}
