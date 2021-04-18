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

import io.infinitic.client.Client
import io.infinitic.common.clients.data.ClientName
import io.infinitic.config.ClientConfig
import io.infinitic.config.data.Transport
import io.infinitic.config.loaders.loadConfigFromFile
import io.infinitic.config.loaders.loadConfigFromResource
import io.infinitic.inMemory.startInMemory
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startClientResponseWorker
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.pulsar.client.api.PulsarClient

@Suppress("unused")
class InfiniticClient private constructor(
    clientName: ClientName
) : Client(clientName) {

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
            val producerName = getProducerName(pulsarClient, clientName)
            val infiniticClient = InfiniticClient(ClientName(producerName))

            val pulsarOutputs = PulsarOutput.from(pulsarClient, pulsarTenant, pulsarNamespace, producerName)
            infiniticClient.setOutput(
                sendToTaskTagEngine = pulsarOutputs.sendToTaskTagEngine(TopicType.COMMANDS, true),
                sendToTaskEngine = pulsarOutputs.sendToTaskEngine(TopicType.COMMANDS, null, true),
                sendToWorkflowTagEngine = pulsarOutputs.sendToWorkflowTagEngine(TopicType.COMMANDS, true),
                sendToWorkflowEngine = pulsarOutputs.sendToWorkflowEngine(TopicType.COMMANDS, true)
            )
            val job = with(CoroutineScope(Dispatchers.IO)) {
                val clientResponseConsumer =
                    PulsarConsumerFactory(pulsarClient, pulsarTenant, pulsarNamespace)
                        .newClientResponseConsumer(producerName, ClientName(producerName))

                startClientResponseWorker(infiniticClient, clientResponseConsumer)
            }

            // close consumer, then the pulsarClient
            infiniticClient.closeFn = {
                job.cancel()
                pulsarClient.close()
            }

            return infiniticClient
        }

        /*
        Create InfiniticClient from a ClientConfig instance
        */
        @JvmStatic
        fun fromConfig(config: ClientConfig): InfiniticClient = when (config.transport) {
            Transport.pulsar -> {
                val pulsarClient = PulsarClient
                    .builder()
                    .serviceUrl(config.pulsar!!.serviceUrl)
                    .build()

                from(
                    pulsarClient,
                    config.pulsar!!.tenant,
                    config.pulsar!!.namespace,
                    config.name
                )
            }

            Transport.inMemory -> {
                // register task and workflows register
                val register = TaskExecutorRegisterImpl()
                config.tasks.map {
                    register.registerTask(it.name) { it.instance }
                }
                config.workflows.map {
                    register.registerWorkflow(it.name) { it.instance }
                }

                val client = InfiniticClient(ClientName(config.name ?: "client: inMemory"))
                client.startInMemory(register)

                client
            }
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
}
