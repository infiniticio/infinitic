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
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.config.WorkerConfig
import io.infinitic.inMemory.transport.InMemoryOutput
import io.infinitic.inMemory.workers.startInMemory
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.engine.storage.WorkflowStateStorage

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticClient(
    workerConfig: WorkerConfig,
    val name: String? = null
) : AbstractInfiniticClient() {

    val taskExecutorRegister = TaskExecutorRegisterImpl()

    init {
        require(workerConfig.tasks.isNotEmpty()) { "No task registered in your config file" }

        workerConfig.tasks.forEach {
            taskExecutorRegister.registerTask(it.name) { it.instance }
        }

        require(workerConfig.workflows.isNotEmpty()) { "No workflow registered in your config file" }

        workerConfig.workflows.forEach {
            taskExecutorRegister.registerWorkflow(it.name) { it.instance }
        }
    }

    override val clientName: ClientName = ClientName(name ?: "client: inMemory")

    private val inMemoryOutput = InMemoryOutput(sendingScope)

    override val sendToTaskTagEngine = inMemoryOutput.sendCommandsToTaskTagEngine

    override val sendToTaskEngine = inMemoryOutput.sendCommandsToTaskEngine

    override val sendToWorkflowTagEngine = inMemoryOutput.sendCommandsToWorkflowTagEngine

    override val sendToWorkflowEngine = inMemoryOutput.sendCommandsToWorkflowEngine

    private val keyValueStorage = InMemoryKeyValueStorage()
    private val keySetStorage = InMemoryKeySetStorage()

    val taskTagStorage: TaskTagStorage = BinaryTaskTagStorage(keyValueStorage, keySetStorage)
    val workflowTagStorage: WorkflowTagStorage = BinaryWorkflowTagStorage(keyValueStorage, keySetStorage)
    val metricsPerNameStorage: MetricsPerNameStateStorage = BinaryMetricsPerNameStateStorage(keyValueStorage)
    val metricsGlobalStorage: MetricsGlobalStateStorage = BinaryMetricsGlobalStateStorage(keyValueStorage)

    val taskStorage: TaskStateStorage = BinaryTaskStateStorage(
        CachedKeyValueStorage(
            workerConfig.stateCache.keyValue(workerConfig),
            workerConfig.stateStorage!!.keyValue(workerConfig),
            cachePersistenceAfterDeletion = MillisDuration(workerConfig.stateCachePersistenceAfterDeletion)
        )
    )

    val workflowStateStorage: WorkflowStateStorage = BinaryWorkflowStateStorage(
        CachedKeyValueStorage(
            workerConfig.stateCache.keyValue(workerConfig),
            workerConfig.stateStorage!!.keyValue(workerConfig),
            cachePersistenceAfterDeletion = MillisDuration(workerConfig.stateCachePersistenceAfterDeletion)
        )
    )

    init {
        runningScope.startInMemory(
            taskExecutorRegister,
            this,
            inMemoryOutput,
            taskTagStorage,
            taskStorage,
            workflowTagStorage,
            workflowStateStorage,
            metricsPerNameStorage,
            metricsGlobalStorage
        ) { }
    }
}
