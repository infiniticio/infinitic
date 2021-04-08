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

private const val N_WORKERS = 10

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    client: Client,
    inMemoryOutput: InMemoryOutput,
    logFn: (_: MessageToProcess<*>) -> Unit
) {
    val keyValueStorage = InMemoryKeyValueStorage()
    val keySetStorage = InMemoryKeySetStorage()

    launch(CoroutineName("logger")) {
        for (messageToProcess in inMemoryOutput.logChannel) {
            logFn(messageToProcess)
        }
    }

    startClientWorker(
        "in-memory-client",
        client,
        inMemoryOutput.clientEventsChannel,
        inMemoryOutput.logChannel,
    )

    startTagEngine(
        "in-memory-tag-engine",
        BinaryTagStateStorage(keyValueStorage, keySetStorage),
        inMemoryOutput.tagCommandsChannel,
        inMemoryOutput.tagEventsChannel,
        inMemoryOutput.logChannel,
        inMemoryOutput.sendEventsToClient,
        inMemoryOutput.sendCommandsToTaskEngine,
        inMemoryOutput.sendCommandsToWorkflowEngine
    )

    startTaskEngine(
        "in-memory-task-engine",
        BinaryTaskStateStorage(keyValueStorage),
        inMemoryOutput.taskCommandsChannel,
        inMemoryOutput.taskEventsChannel,
        inMemoryOutput.logChannel,
        inMemoryOutput.sendEventsToClient,
        inMemoryOutput.sendEventsToTagEngine,
        inMemoryOutput.sendEventsToTaskEngine,
        inMemoryOutput.sendEventsToWorkflowEngine,
        inMemoryOutput.sendToTaskExecutors,
        inMemoryOutput.sendToMetricsPerName
    )

    startWorkflowEngine(
        "in-memory-workflow-engine",
        BinaryWorkflowStateStorage(keyValueStorage),
        inMemoryOutput.workflowCommandsChannel,
        inMemoryOutput.workflowEventsChannel,
        inMemoryOutput.logChannel,
        inMemoryOutput.sendEventsToClient,
        inMemoryOutput.sendEventsToTagEngine,
        inMemoryOutput.sendCommandsToTaskEngine,
        inMemoryOutput.sendEventsToWorkflowEngine
    )

    repeat(N_WORKERS) {
        startTaskExecutor(
            "in-memory-task-executor-$it",
            taskExecutorRegister,
            inMemoryOutput.executorChannel,
            inMemoryOutput.logChannel,
            inMemoryOutput.sendEventsToTaskEngine
        )
    }

    startMetricsPerNameEngine(
        "in-memory-monitoring-per-name-engine",
        BinaryMetricsPerNameStateStorage(keyValueStorage),
        inMemoryOutput.metricsPerNameChannel,
        inMemoryOutput.logChannel,
        inMemoryOutput.sendToMetricsGlobal
    )

    startMetricsGlobalEngine(
        "in-memory-monitoring-global-engine",
        BinaryMetricsGlobalStateStorage(keyValueStorage),
        inMemoryOutput.metricsGlobalChannel,
        inMemoryOutput.logChannel
    )
}
