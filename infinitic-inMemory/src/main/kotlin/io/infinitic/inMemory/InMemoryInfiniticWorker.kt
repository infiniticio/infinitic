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

import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.inMemory.transport.InMemoryOutput
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.worker.startMetricsGlobalEngine
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.worker.MetricsPerNameMessageToProcess
import io.infinitic.metrics.perName.engine.worker.startMetricsPerNameEngine
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.infinitic.tags.tasks.worker.TaskTagEngineMessageToProcess
import io.infinitic.tags.tasks.worker.startTaskTagEngine
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tags.workflows.worker.WorkflowTagEngineMessageToProcess
import io.infinitic.tags.workflows.worker.startWorkflowTagEngine
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.tasks.engine.worker.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.worker.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.worker.InfiniticWorker
import io.infinitic.worker.config.WorkerConfig
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.workflows.engine.worker.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticWorker(
    workerConfig: WorkerConfig
) : InfiniticWorker(workerConfig) {

    lateinit var output: InMemoryOutput
    lateinit var client: InMemoryInfiniticClient
    override lateinit var name: String

    override fun startAsync(): CompletableFuture<Unit> =
        if (this::output.isInitialized && this::client.isInitialized && this::name.isInitialized) {
            super.startAsync()
        } else {
            logger.warn { "Should not start ${InMemoryInfiniticWorker::class.java.name} outside of an in-memory client - Closing" }
            CompletableFuture.completedFuture(null)
        }

    override fun startTaskExecutors(name: Name, concurrency: Int) {
        when (name) {
            is TaskName ->
                {
                    val channel = Channel<TaskExecutorMessageToProcess>()
                    output.taskExecutorChannel[name] = channel

                    repeat(concurrency) {
                        runningScope.launch {
                            startTaskExecutor(
                                "task-executor-$it: $name",
                                taskExecutorRegister,
                                inputChannel = channel,
                                outputChannel = output.logChannel,
                                output.sendEventsToTaskEngine(name)
                            ) { client }
                        }
                    }
                }
            is WorkflowName ->
                {
                    val channel = Channel<TaskExecutorMessageToProcess>()
                    output.workflowTaskExecutorChannel[name] = channel

                    repeat(concurrency) {
                        runningScope.launch {
                            startTaskExecutor(
                                "workflow-task-executor-$it: $name",
                                taskExecutorRegister,
                                inputChannel = channel,
                                outputChannel = output.logChannel,
                                output.sendEventsToTaskEngine(name)
                            ) { client }
                        }
                    }
                }
            else -> thisShouldNotHappen()
        }
    }

    override fun startTaskTagEngines(taskName: TaskName, concurrency: Int, storage: TaskTagStorage) {
        val channel = Channel<TaskTagEngineMessageToProcess>()
        output.taskTagChannel[taskName] = channel

        runningScope.launch {
            startTaskTagEngine(
                "task-tag-engine: $name",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendCommandsToTaskEngine(taskName),
                output.sendToClient
            )
        }
    }

    override fun startTaskEngines(taskName: TaskName, concurrency: Int, storage: TaskStateStorage) {
        val channel = Channel<TaskEngineMessageToProcess>()
        output.taskChannel[taskName] = channel

        runningScope.launch {
            startTaskEngine(
                "task-engine: $taskName",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendToClient,
                output.sendEventsToTaskTagEngine,
                output.sendToTaskEngineAfter(taskName),
                output.sendEventsToWorkflowEngine,
                output.sendToTaskExecutors(taskName),
                output.sendToMetricsPerName(taskName)
            )
        }
    }

    override fun startTaskDelayEngines(taskName: TaskName, concurrency: Int) {
        // not needed
    }

    override fun startWorkflowTagEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowTagStorage) {
        val channel = Channel<WorkflowTagEngineMessageToProcess>()
        output.workflowTagChannel[workflowName] = channel

        runningScope.launch {
            startWorkflowTagEngine(
                "workflow-tag-engine: $name",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendCommandsToWorkflowEngine,
                output.sendToClient
            )
        }
    }

    override fun startWorkflowEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowStateStorage) {
        val channel = Channel<WorkflowEngineMessageToProcess>()
        output.workflowChannel[workflowName] = channel

        runningScope.launch {
            startWorkflowEngine(
                "workflow-engine: $name",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendToClient,
                output.sendCommandsToTaskTagEngine,
                {
                    when (it.isWorkflowTask()) {
                        true -> output.sendCommandsToTaskEngine(workflowName)(it)
                        false -> output.sendCommandsToTaskEngine(it.taskName)(it)
                    }
                },
                output.sendEventsToWorkflowTagEngine,
                output.sendEventsToWorkflowEngine,
                output.sendToWorkflowEngineAfter
            )
        }
    }

    override fun startWorkflowDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        // Implementation not needed
    }

    override fun startTaskEngines(workflowName: WorkflowName, concurrency: Int, storage: TaskStateStorage) {
        val channel = Channel<TaskEngineMessageToProcess>()
        output.workflowTaskChannel[workflowName] = channel

        runningScope.launch {
            startTaskEngine(
                "workflow-task-engine: $workflowName",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendToClient,
                output.sendEventsToTaskTagEngine,
                output.sendToTaskEngineAfter(workflowName),
                output.sendEventsToWorkflowEngine,
                output.sendToTaskExecutors(workflowName),
                output.sendToMetricsPerName(workflowName)
            )
        }
    }

    override fun startTaskDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        // Implementation not needed
    }

    override fun startMetricsPerNameEngines(taskName: TaskName, storage: MetricsPerNameStateStorage) {
        val channel = Channel<MetricsPerNameMessageToProcess>()
        output.taskMetricsPerNameChannel[taskName] = channel

        runningScope.launch {
            startMetricsPerNameEngine(
                "metrics-per-name-engine",
                storage,
                inputChannel = channel,
                outputChannel = output.logChannel,
                output.sendToMetricsGlobal
            )
        }
    }

    override fun startMetricsGlobalEngine(storage: MetricsGlobalStateStorage) {
        runningScope.launch {
            startMetricsGlobalEngine(
                "metrics-global-engine",
                storage,
                inputChannel = output.metricsGlobalChannel,
                outputChannel = output.logChannel
            )
        }
    }
}
