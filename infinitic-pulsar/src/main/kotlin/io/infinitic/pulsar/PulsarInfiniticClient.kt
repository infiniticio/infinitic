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

import io.infinitic.clients.InfiniticClientAbstract
import io.infinitic.common.data.ClientName
import io.infinitic.exceptions.clients.ExceptionAtInitialization
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.transport.pulsar.PulsarStarter
import io.infinitic.transport.pulsar.topics.ClientTopics
import io.infinitic.transport.pulsar.topics.PerNameTopics
import kotlinx.coroutines.future.future
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.api.PulsarClient

@Suppress("unused", "MemberVisibilityCanBePrivate")
class PulsarInfiniticClient @JvmOverloads constructor(
    val pulsarClient: PulsarClient,
    val pulsarAdmin: PulsarAdmin,
    pulsarTenant: String,
    pulsarNamespace: String,
    name: String? = null
) : InfiniticClientAbstract() {

    private val topics = PerNameTopics(pulsarTenant, pulsarNamespace)

    /**
     * If [name] is provided, we check that no other client is connected with it
     * If [name] is not provided, Pulsar provided a unique name
     */
    private val uniqueName by lazy { getProducerName(pulsarClient, topics, name) }

    override val clientName by lazy { ClientName(uniqueName) }

    /**
     * Name of the topic used to send messages to this client
     */
    private val topicClient by lazy { topics.topic(ClientTopics.RESPONSE, clientName) }

    override val clientStarter by lazy { PulsarStarter(pulsarClient, topics, uniqueName) }

    init {
        // create client's topic
        try {
            logger.debug { "Creating topic $topicClient" }
            pulsarAdmin.topics().createNonPartitionedTopic(topicClient)
        } catch (e: PulsarAdminException.ConflictException) {
            logger.debug { "Topic already exists: $topicClient: ${e.message}" }
        } catch (e: PulsarAdminException.NotAllowedException) {
            logger.warn { "Not allowed to create topic $topicClient: ${e.message}" }
        } catch (e: PulsarAdminException.NotAuthorizedException) {
            logger.warn { "Not authorized to create topic $topicClient: ${e.message}" }
        } catch (e: Exception) {
            logger.warn(e) {}
            close()
            throw ExceptionAtInitialization
        }

        with(clientStarter) {
            listeningScope.future {
                startClientResponse(this@PulsarInfiniticClient)
            }
            this
        }
    }

    override fun close() {
        super.close()

        // force delete client's topic
        try {
            logger.debug { "Deleting topic $topicClient" }
            pulsarAdmin.topics().delete(topicClient, true)
        } catch (e: Exception) {
            logger.warn { "Error while deleting topic $topicClient: $e" }
        }

        try {
            pulsarClient.close()
        } catch (e: Exception) {
            logger.warn { "Error while closing Pulsar client: $e" }
        }

        try {
            pulsarAdmin.close()
        } catch (e: Exception) {
            logger.warn { "Error while closing Pulsar admin: $e" }
        }
    }

    companion object {
        /**
         * Create PulsarInfiniticClient from a custom PulsarClient and a ClientConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, pulsarAdmin: PulsarAdmin, clientConfig: ClientConfig) =
            PulsarInfiniticClient(
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
        fun fromConfig(clientConfig: ClientConfig) =
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
