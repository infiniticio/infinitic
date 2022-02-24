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

import io.infinitic.client.InfiniticClient
import io.infinitic.client.worker.startClientWorker
import io.infinitic.common.data.ClientName
import io.infinitic.transport.inMemory.InMemoryOutput
import io.infinitic.worker.config.WorkerConfig
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticClient(
    workerConfig: WorkerConfig,
    name: String? = null
) : InfiniticClient() {

    private val inMemoryOutput = InMemoryOutput(sendingScope)

    private val worker by lazy {
        InMemoryInfiniticWorker(workerConfig).apply {
            output = inMemoryOutput
            client = this@InMemoryInfiniticClient
            this.name = clientName.toString()
        }
    }

    override val clientName: ClientName = ClientName(name ?: "inMemory")

    override val sendToTaskTagEngine = inMemoryOutput.sendCommandsToTaskTagEngine

    override val sendToTaskEngine = inMemoryOutput.sendCommandsToTaskEngine()

    override val sendToWorkflowTagEngine = inMemoryOutput.sendCommandsToWorkflowTagEngine

    override val sendToWorkflowEngine = inMemoryOutput.sendCommandsToWorkflowEngine

    init {
        runningScope.launch {
            launch(CoroutineName("client-logger")) {
                @Suppress("ControlFlowWithEmptyBody")
                for (messageToProcess in inMemoryOutput.logChannel) {
                    // just clear the channel
                }
            }

            startClientWorker(
                this@InMemoryInfiniticClient,
                inputChannel = inMemoryOutput.clientChannel,
                outputChannel = inMemoryOutput.logChannel,
            )
        }

        worker.startAsync()
    }

    override fun close() {
        super.close()

        worker.close()
    }
}
