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

import io.infinitic.client.Client
import io.infinitic.client.worker.startClientWorker
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.inMemory.transport.InMemoryOutput
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.worker.startMetricsGlobalEngine
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.worker.startMetricsPerNameEngine
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.engine.storage.BinaryTagStateStorage
import io.infinitic.tags.engine.worker.startTagEngine
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val N_TASK_EXECUTORS = 10

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    client: Client,
    output: InMemoryOutput,
    logFn: (_: MessageToProcess<*>) -> Unit
) = launch {
    val keyValueStorage = InMemoryKeyValueStorage()
    val keySetStorage = InMemoryKeySetStorage()

    launch(CoroutineName("logger")) {
        for (messageToProcess in output.logChannel) {
            logFn(messageToProcess)
        }
    }

    startClientWorker(
        client,
        inputChannel = output.clientEventsChannel,
        outputChannel = output.logChannel,
    )

    startTagEngine(
        "in-memory-tag-engine",
        BinaryTagStateStorage(keyValueStorage, keySetStorage),
        eventsInputChannel = output.tagEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.tagCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendEventsToClient,
        output.sendCommandsToTaskEngine,
        output.sendCommandsToWorkflowEngine
    )

    startTaskEngine(
        "in-memory-task-engine",
        BinaryTaskStateStorage(keyValueStorage),
        eventsInputChannel = output.taskEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.taskCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendEventsToClient,
        output.sendEventsToTagEngine,
        output.sendEventsToTaskEngine,
        output.sendEventsToWorkflowEngine,
        output.sendToTaskExecutors,
        output.sendToMetricsPerName
    )

    startWorkflowEngine(
        "in-memory-workflow-engine",
        BinaryWorkflowStateStorage(keyValueStorage),
        eventsInputChannel = output.workflowEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.workflowCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendEventsToClient,
        output.sendEventsToTagEngine,
        output.sendCommandsToTaskEngine,
        output.sendEventsToWorkflowEngine
    )

    repeat(N_TASK_EXECUTORS) {
        startTaskExecutor(
            "in-memory-task-executor-$it",
            taskExecutorRegister,
            inputChannel = output.executorChannel,
            outputChannel = output.logChannel,
            output.sendEventsToTaskEngine,
        )
    }

    startMetricsPerNameEngine(
        "in-memory-metrics-per-name-engine",
        BinaryMetricsPerNameStateStorage(keyValueStorage),
        inputChannel = output.metricsPerNameChannel,
        outputChannel = output.logChannel,
        output.sendToMetricsGlobal
    )

    startMetricsGlobalEngine(
        "in-memory-metrics-global-engine",
        BinaryMetricsGlobalStateStorage(keyValueStorage),
        inputChannel = output.metricsGlobalChannel,
        outputChannel = output.logChannel
    )
}
