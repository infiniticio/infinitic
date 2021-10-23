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
import io.infinitic.common.data.ClientName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startClientResponseWorker
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class PulsarInfiniticClient @JvmOverloads constructor(
    val pulsarClient: PulsarClient,
    val pulsarAdmin: PulsarAdmin,
    val pulsarTenant: String,
    val pulsarNamespace: String,
    name: String? = null
) : InfiniticClient() {

    private val producerName by lazy { getProducerName(pulsarClient, pulsarTenant, pulsarNamespace, name) }

    override val clientName by lazy { ClientName(producerName) }

    private val topicClient by lazy { TopicName(pulsarTenant, pulsarNamespace).of(clientName) }

    private val pulsarOutput by lazy {
        // create client's topic
        pulsarAdmin.topics().createNonPartitionedTopicAsync(topicClient).join()

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

        // force delete client's topic
        try {
            pulsarAdmin.topics().delete(topicClient, true)
        } catch (e: PulsarClientException.TopicDoesNotExistException) {
            // ignore
        }

        pulsarClient.close()
        pulsarAdmin.close()
    }

    companion object {
        /**
         * Create PulsarInfiniticClient from a custom PulsarClient and a ClientConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, pulsarAdmin: PulsarAdmin, clientConfig: ClientConfig) = PulsarInfiniticClient(
            pulsarClient,
            pulsarAdmin,
            clientConfig.pulsar!!.tenant,
            clientConfig.pulsar.namespace,
            clientConfig.name
        )

        /**
         * Create PulsarInfiniticClient from a ClientConfig instance
         */
        @JvmStatic
        fun fromConfig(clientConfig: ClientConfig): InfiniticClient =
            from(clientConfig.pulsar!!.client, clientConfig.pulsar.admin, clientConfig)

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
