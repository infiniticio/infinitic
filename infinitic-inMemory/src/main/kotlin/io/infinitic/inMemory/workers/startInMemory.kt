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

package io.infinitic.inMemory.workers

import io.infinitic.cache.no.NoCache
import io.infinitic.client.Client
import io.infinitic.client.worker.startClientWorker
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.ClientResponseMessageToProcess
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.inMemory.transport.InMemoryMonitoringPerNameOutput
import io.infinitic.inMemory.transport.InMemoryTaskEngineOutput
import io.infinitic.inMemory.transport.InMemoryTaskExecutorOutput
import io.infinitic.inMemory.transport.InMemoryWorkflowEngineOutput
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalInputChannels
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalMessageToProcess
import io.infinitic.monitoring.global.engine.worker.startMonitoringGlobalEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameInputChannels
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineInputChannels
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.workflows.engine.storage.events.NoWorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateKeyValueStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineInputChannels
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.lang.RuntimeException

private const val N_WORKERS = 10

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    keyValueStorage: KeyValueStorage,
    client: Client,
    clientResponsesChannel: Channel<ClientResponseMessageToProcess>,
    taskEngineCommandsChannel: Channel<TaskEngineMessageToProcess>,
    workflowEngineCommandsChannel: Channel<WorkflowEngineMessageToProcess>
) = launch(Dispatchers.IO) {

    val logChannel = Channel<MessageToProcess<Any>>()
    val workflowEngineEventsChannel = Channel<WorkflowEngineMessageToProcess>()
    val taskEngineEventsChannel = Channel<TaskEngineMessageToProcess>()
    val taskExecutorChannel = Channel<TaskExecutorMessageToProcess>()
    val monitoringPerNameChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringGlobalChannel = Channel<MonitoringGlobalMessageToProcess>()

    launch(CoroutineName("logger")) {
        for (messageToProcess in logChannel) {
            when (val message = messageToProcess.message) {
                is MonitoringGlobalMessage ->
                    println("Monitoring Global  : $message")
                is MonitoringPerNameEngineMessage ->
                    println("Monitoring Per Name: $message")
                is TaskExecutorMessage ->
                    println("Task Executor      : $message")
                is TaskEngineMessage ->
                    println("Task engine        : $message")
                is WorkflowEngineMessage ->
                    println("Workflow engine    : $message")
                is ClientResponseMessage ->
                    println("Client response    : $message")
                else -> throw RuntimeException("Unknown messageToProcess type: $messageToProcess")
            }
        }
    }

    startClientWorker(
        "in-memory-client",
        client,
        clientResponsesChannel,
        logChannel,
    )

    startWorkflowEngine(
        "in-memory-workflow-engine",
        WorkflowStateKeyValueStorage(keyValueStorage, NoCache()),
        NoWorkflowEventStorage(),
        WorkflowEngineInputChannels(
            workflowEngineCommandsChannel,
            workflowEngineEventsChannel,
            logChannel
        ),
        InMemoryWorkflowEngineOutput(
            this,
            clientResponsesChannel,
            taskEngineCommandsChannel,
            workflowEngineEventsChannel
        )
    )

    startTaskEngine(
        "in-memory-task-engine",
        TaskStateKeyValueStorage(keyValueStorage, NoCache()),
        NoTaskEventStorage(),
        TaskEngineInputChannels(
            taskEngineCommandsChannel,
            taskEngineEventsChannel,
            logChannel
        ),
        InMemoryTaskEngineOutput(
            this,
            clientResponsesChannel,
            taskEngineEventsChannel,
            taskExecutorChannel,
            monitoringPerNameChannel,
            workflowEngineEventsChannel
        )
    )

    repeat(N_WORKERS) {
        startTaskExecutor(
            "in-memory-task-executor-$it",
            taskExecutorRegister,
            TaskExecutorInput(taskExecutorChannel, logChannel),
            InMemoryTaskExecutorOutput(this, taskEngineEventsChannel),
        )
    }

    startMonitoringPerNameEngine(
        "in-memory-monitoring-per-name-engine",
        MonitoringPerNameStateKeyValueStorage(keyValueStorage, NoCache()),
        MonitoringPerNameInputChannels(
            monitoringPerNameChannel,
            logChannel
        ),
        InMemoryMonitoringPerNameOutput(
            this,
            monitoringGlobalChannel
        )
    )

    startMonitoringGlobalEngine(
        "in-memory-monitoring-global-engine",
        MonitoringGlobalStateKeyValueStorage(keyValueStorage, NoCache()),
        MonitoringGlobalInputChannels(
            monitoringGlobalChannel,
            logChannel
        )
    )
}
