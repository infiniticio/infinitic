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

import io.infinitic.client.Client
import io.infinitic.common.clients.data.ClientName
import io.infinitic.inMemory.transport.InMemoryOutput
import io.infinitic.inMemory.workers.startInMemory
import io.infinitic.tasks.TaskExecutorRegister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class InfiniticClient(
    taskExecutorRegister: TaskExecutorRegister,
    val name: String? = null
) : Client() {

    private val job: Job

    override val clientName: ClientName = ClientName(name ?: "client: inMemory")

    private val threadPool = Executors.newCachedThreadPool()

    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + Job())

    private val inMemoryOutput = InMemoryOutput(scope)

    override val sendToTaskTagEngine = inMemoryOutput.sendCommandsToTaskTagEngine

    override val sendToTaskEngine = inMemoryOutput.sendCommandsToTaskEngine

    override val sendToWorkflowTagEngine = inMemoryOutput.sendCommandsToWorkflowTagEngine

    override val sendToWorkflowEngine = inMemoryOutput.sendCommandsToWorkflowEngine

    override fun close() {
        job.cancel()
        threadPool.shutdown()
    }

    init {
        job = scope.startInMemory(taskExecutorRegister, this, inMemoryOutput) { }
    }
}
