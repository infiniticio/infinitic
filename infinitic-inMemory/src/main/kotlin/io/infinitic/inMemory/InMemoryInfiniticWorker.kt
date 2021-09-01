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
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.thisShouldNotHappen
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticWorker(
    workerConfig: WorkerConfig
) : InfiniticWorker(workerConfig) {

    lateinit var output: InMemoryOutput
    lateinit var client: InMemoryInfiniticClient
    override lateinit var name: String

    override fun start() {
        if (this::output.isInitialized && this::client.isInitialized && this::name.isInitialized) {
            super.start()
        } else {
            logger.info { "Can not start ${InMemoryInfiniticWorker::class.java.name} outside of in-memory client - Closing" }
        }
    }

    override fun CoroutineScope.startTaskExecutors(name: Name, concurrency: Int) {
        when (name) {
            is TaskName ->
                {
                    val channel = Channel<TaskExecutorMessageToProcess>()
                    output.taskExecutorChannel[name] = channel

                    repeat(concurrency) {
                        startTaskExecutor(
                            "in-memory-task-executor-$it: $name",
                            taskExecutorRegister,
                            inputChannel = channel,
                            outputChannel = output.logChannel,
                            output.sendEventsToTaskEngine(name),
                            { client }
                        )
                    }
                }
            is WorkflowName ->
                {
                    val channel = Channel<TaskExecutorMessageToProcess>()
                    output.workflowTaskExecutorChannel[name] = channel

                    repeat(concurrency) {
                        startTaskExecutor(
                            "in-memory-workflow-task-executor-$it: $name",
                            taskExecutorRegister,
                            inputChannel = channel,
                            outputChannel = output.logChannel,
                            output.sendEventsToTaskEngine(name),
                            { client }
                        )
                    }
                }
            else -> thisShouldNotHappen()
        }
    }

    override fun CoroutineScope.startTaskTagEngines(taskName: TaskName, concurrency: Int, storage: TaskTagStorage) {
        val commandsChannel = Channel<TaskTagEngineMessageToProcess>()
        val eventsChannel = Channel<TaskTagEngineMessageToProcess>()

        output.taskTagCommandsChannel[taskName] = commandsChannel
        output.taskTagEventsChannel[taskName] = eventsChannel

        startTaskTagEngine(
            "in-memory-task-tag-engine: $taskName",
            storage,
            eventsInputChannel = eventsChannel,
            eventsOutputChannel = output.logChannel,
            commandsInputChannel = commandsChannel,
            commandsOutputChannel = output.logChannel,
            output.sendCommandsToTaskEngine(taskName),
            output.sendToClient
        )
    }

    override fun CoroutineScope.startTaskEngines(taskName: TaskName, concurrency: Int, storage: TaskStateStorage) {
        val commandsChannel = Channel<TaskEngineMessageToProcess>()
        val eventsChannel = Channel<TaskEngineMessageToProcess>()

        output.taskCommandsChannel[taskName] = commandsChannel
        output.taskEventsChannel[taskName] = eventsChannel

        startTaskEngine(
            "in-memory-task-engine: $taskName",
            storage,
            eventsInputChannel = eventsChannel,
            eventsOutputChannel = output.logChannel,
            commandsInputChannel = commandsChannel,
            commandsOutputChannel = output.logChannel,
            output.sendToClient,
            output.sendEventsToTaskTagEngine,
            output.sendToTaskEngineAfter(taskName),
            output.sendEventsToWorkflowEngine,
            output.sendToTaskExecutors(taskName),
            output.sendToMetricsPerName(taskName)
        )
    }

    override fun CoroutineScope.startTaskDelayEngines(taskName: TaskName, concurrency: Int) {
        // Implementation not needed
    }

    override fun CoroutineScope.startWorkflowTagEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowTagStorage) {
        val commandsChannel = Channel<WorkflowTagEngineMessageToProcess>()
        val eventsChannel = Channel<WorkflowTagEngineMessageToProcess>()

        output.workflowTagCommandsChannel[workflowName] = commandsChannel
        output.workflowTagEventsChannel[workflowName] = eventsChannel

        startWorkflowTagEngine(
            "in-memory-workflow-tag-engine: $workflowName",
            storage,
            eventsInputChannel = eventsChannel,
            eventsOutputChannel = output.logChannel,
            commandsInputChannel = commandsChannel,
            commandsOutputChannel = output.logChannel,
            output.sendCommandsToWorkflowEngine,
            output.sendToClient
        )
    }

    override fun CoroutineScope.startWorkflowEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowStateStorage) {
        val commandsChannel = Channel<WorkflowEngineMessageToProcess>()
        val eventsChannel = Channel<WorkflowEngineMessageToProcess>()

        output.workflowCommandsChannel[workflowName] = commandsChannel
        output.workflowEventsChannel[workflowName] = eventsChannel

        startWorkflowEngine(
            "in-memory-workflow-engine",
            storage,
            eventsInputChannel = eventsChannel,
            eventsOutputChannel = output.logChannel,
            commandsInputChannel = commandsChannel,
            commandsOutputChannel = output.logChannel,
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

    override fun CoroutineScope.startWorkflowDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        // Implementation not needed
    }

    override fun CoroutineScope.startTaskEngines(workflowName: WorkflowName, concurrency: Int, storage: TaskStateStorage) {
        val commandsChannel = Channel<TaskEngineMessageToProcess>()
        val eventsChannel = Channel<TaskEngineMessageToProcess>()

        output.workflowTaskCommandsChannel[workflowName] = commandsChannel
        output.workflowTaskEventsChannel[workflowName] = eventsChannel

        startTaskEngine(
            "in-memory-workflow-task-engine: $workflowName",
            storage,
            eventsInputChannel = eventsChannel,
            eventsOutputChannel = output.logChannel,
            commandsInputChannel = commandsChannel,
            commandsOutputChannel = output.logChannel,
            output.sendToClient,
            output.sendEventsToTaskTagEngine,
            output.sendToTaskEngineAfter(workflowName),
            output.sendEventsToWorkflowEngine,
            output.sendToTaskExecutors(workflowName),
            output.sendToMetricsPerName(workflowName)
        )
    }

    override fun CoroutineScope.startTaskDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        // Implementation not needed
    }

    override fun CoroutineScope.startMetricsPerNameEngines(taskName: TaskName, storage: MetricsPerNameStateStorage) {
        val channel = Channel<MetricsPerNameMessageToProcess>()
        output.taskMetricsPerNameChannel[taskName] = channel

        startMetricsPerNameEngine(
            "in-memory-metrics-per-name-engine",
            storage,
            inputChannel = channel,
            outputChannel = output.logChannel,
            output.sendToMetricsGlobal
        )
    }

    override fun CoroutineScope.startMetricsGlobalEngine(storage: MetricsGlobalStateStorage) {
        startMetricsGlobalEngine(
            "in-memory-metrics-global-engine",
            storage,
            inputChannel = output.metricsGlobalChannel,
            outputChannel = output.logChannel
        )
    }
}
