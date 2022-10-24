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

package io.infinitic.clients

import io.infinitic.inMemory.InMemoryInfiniticClient
import io.infinitic.pulsar.PulsarInfiniticClient
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.registers.WorkerRegisterImpl

@Suppress("unused")
class InfiniticClient private constructor(
    private val client: InfiniticClientAbstract
) : InfiniticClientInterface by client {

    companion object {
        /**
         * Create InfiniticClient with config from resources directory
         */
        @JvmStatic
        @JvmOverloads
        fun fromConfig(clientConfig: ClientConfig, workerConfig: WorkerConfig? = null): InfiniticClient {
            return InfiniticClient(
                when (clientConfig.transport) {
                    Transport.pulsar -> PulsarInfiniticClient.fromConfig(clientConfig)
                    Transport.inMemory -> InMemoryInfiniticClient(WorkerRegisterImpl(workerConfig!!))
                }
            )
        }

        /**
         * Create InfiniticClient with config from resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String): InfiniticClient {
            val clientConfig = ClientConfig.fromResource(*resources)

            return InfiniticClient(
                when (clientConfig.transport) {
                    Transport.pulsar -> PulsarInfiniticClient.fromConfig(clientConfig)
                    Transport.inMemory -> InMemoryInfiniticClient(WorkerRegisterImpl(WorkerConfig.fromResource(*resources)))
                }
            )
        }

        /**
         * Create InfiniticClient with config from system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String): InfiniticClient {
            val clientConfig = ClientConfig.fromFile(*files)

            return InfiniticClient(
                when (clientConfig.transport) {
                    Transport.pulsar -> PulsarInfiniticClient.fromConfig(clientConfig)
                    Transport.inMemory -> InMemoryInfiniticClient(WorkerRegisterImpl(WorkerConfig.fromFile(*files)))
                }
            )
        }
    }
}
