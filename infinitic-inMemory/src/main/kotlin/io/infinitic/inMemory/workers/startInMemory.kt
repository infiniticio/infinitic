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
import io.infinitic.common.clients.transport.ClientResponseMessageToProcess
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.inMemory.transport.InMemoryMonitoringPerNameOutput
import io.infinitic.inMemory.transport.InMemoryTagEngineOutput
import io.infinitic.inMemory.transport.InMemoryTaskEngineOutput
import io.infinitic.inMemory.transport.InMemoryTaskExecutorOutput
import io.infinitic.inMemory.transport.InMemoryWorkflowEngineOutput
import io.infinitic.monitoring.global.engine.input.MonitoringGlobalInputChannels
import io.infinitic.monitoring.global.engine.input.MonitoringGlobalMessageToProcess
import io.infinitic.monitoring.global.engine.storage.BinaryMonitoringGlobalStateStorage
import io.infinitic.monitoring.global.engine.worker.startMonitoringGlobalEngine
import io.infinitic.monitoring.perName.engine.input.MonitoringPerNameInputChannels
import io.infinitic.monitoring.perName.engine.input.MonitoringPerNameMessageToProcess
import io.infinitic.monitoring.perName.engine.storage.BinaryMonitoringPerNameStateStorage
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.storage.inMemory.keySet.InMemoryKeySetStorage
import io.infinitic.tags.engine.input.TagEngineInputChannels
import io.infinitic.tags.engine.input.TagEngineMessageToProcess
import io.infinitic.tags.engine.storage.CachedKeyTagStateStorage
import io.infinitic.tags.engine.worker.startTagEngine
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.input.TaskEngineInputChannels
import io.infinitic.tasks.engine.input.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.storage.states.CachedKeyTaskStateStorage
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.workflows.engine.input.WorkflowEngineInputChannels
import io.infinitic.workflows.engine.input.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.storage.states.CachedKeyWorkflowStateStorage
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val N_WORKERS = 10

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    keyValueStorage: KeyValueStorage,
    client: Client,
    tagEngineCommandsChannel: Channel<TagEngineMessageToProcess>,
    taskEngineCommandsChannel: Channel<TaskEngineMessageToProcess>,
    workflowEngineCommandsChannel: Channel<WorkflowEngineMessageToProcess>,
    logFn: (_: MessageToProcess<*>) -> Unit = { }
) {
    val logChannel = Channel<MessageToProcess<Any>>()
    val clientResponsesChannel = Channel<ClientResponseMessageToProcess>()
    val tagEngineEventsChannel = Channel<TagEngineMessageToProcess>()
    val taskEngineEventsChannel = Channel<TaskEngineMessageToProcess>()
    val workflowEngineEventsChannel = Channel<WorkflowEngineMessageToProcess>()
    val taskExecutorChannel = Channel<TaskExecutorMessageToProcess>()
    val monitoringPerNameChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringGlobalChannel = Channel<MonitoringGlobalMessageToProcess>()

    launch(CoroutineName("logger")) {
        for (messageToProcess in logChannel) {
            logFn(messageToProcess)
        }
    }

    startClientWorker(
        "in-memory-client",
        client,
        clientResponsesChannel,
        logChannel,
    )

    startTagEngine(
        "in-memory-tag-engine",
        CachedKeyTagStateStorage(keyValueStorage, NoCache(), InMemoryKeySetStorage(), NoCache()),
        TagEngineInputChannels(
            tagEngineCommandsChannel,
            tagEngineEventsChannel,
            logChannel
        ),
        InMemoryTagEngineOutput(
            this,
            clientResponsesChannel,
            taskEngineCommandsChannel,
            workflowEngineCommandsChannel
        )
    )

    startTaskEngine(
        "in-memory-task-engine",
        CachedKeyTaskStateStorage(keyValueStorage, NoCache()),
        TaskEngineInputChannels(
            taskEngineCommandsChannel,
            taskEngineEventsChannel,
            logChannel
        ),
        InMemoryTaskEngineOutput(
            this,
            clientResponsesChannel,
            tagEngineEventsChannel,
            taskEngineEventsChannel,
            taskExecutorChannel,
            monitoringPerNameChannel,
            workflowEngineEventsChannel
        )
    )

    startWorkflowEngine(
        "in-memory-workflow-engine",
        CachedKeyWorkflowStateStorage(keyValueStorage, NoCache()),
        WorkflowEngineInputChannels(
            workflowEngineCommandsChannel,
            workflowEngineEventsChannel,
            logChannel
        ),
        InMemoryWorkflowEngineOutput(
            this,
            clientResponsesChannel,
            tagEngineEventsChannel,
            taskEngineCommandsChannel,
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
        BinaryMonitoringPerNameStateStorage(keyValueStorage),
        MonitoringPerNameInputChannels(
            monitoringPerNameChannel,
            logChannel
        ),
        InMemoryMonitoringPerNameOutput(
            monitoringGlobalChannel
        )
    )

    startMonitoringGlobalEngine(
        "in-memory-monitoring-global-engine",
        BinaryMonitoringGlobalStateStorage(keyValueStorage),
        MonitoringGlobalInputChannels(
            monitoringGlobalChannel,
            logChannel
        )
    )
}
