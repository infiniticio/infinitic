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

package io.infinitic.inMemory

import io.infinitic.client.AbstractInfiniticClient
import io.infinitic.common.data.ClientName
import io.infinitic.workers.config.WorkerConfig

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticClient(
    workerConfig: WorkerConfig,
    name: String? = null
) : AbstractInfiniticClient() {

    private val worker by lazy {
        InMemoryInfiniticWorker(workerConfig).apply {
            client = this@InMemoryInfiniticClient
            runningScope = this@InMemoryInfiniticClient.runningScope
        }
    }

    private val workerStarter by lazy { worker.workerStarter }

    override val clientName: ClientName = ClientName(name ?: "inMemory")

    override val sendToTaskTagEngine = workerStarter.sendToTaskTag

    override val sendToTaskEngine = workerStarter.sendToTaskEngine

    override val sendToWorkflowTagEngine = workerStarter.sendToWorkflowTag

    override val sendToWorkflowEngine = workerStarter.sendToWorkflowEngine

    init {
        // start client response
        with(workerStarter) {
            runningScope.startClientResponse(this@InMemoryInfiniticClient)
        }

        // start worker
        with(worker) {
            runningScope.startAsync()
        }
    }
}
