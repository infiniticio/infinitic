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

package io.infinitic.factory

import io.infinitic.inMemory.InMemoryInfiniticWorker
import io.infinitic.pulsar.PulsarInfiniticWorker
import io.infinitic.transport.config.Transport
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfig

@Suppress("unused")
object InfiniticWorkerFactory {
    /**
     * Create InfiniticWorker with config from resources directory
     */
    @JvmStatic
    fun fromConfigResource(vararg resources: String): InfiniticWorker = fromConfig(WorkerConfig.fromResource(*resources))

    /**
     * Create InfiniticWorker with config from system file
     */
    @JvmStatic
    fun fromConfigFile(vararg files: String): InfiniticWorker = fromConfig(WorkerConfig.fromFile(*files))

    private fun fromConfig(workerConfig: WorkerConfig) = when (workerConfig.transport) {
        Transport.pulsar -> PulsarInfiniticWorker.fromConfig(workerConfig)
        Transport.inMemory -> InMemoryInfiniticWorker(workerConfig)
    }
}
