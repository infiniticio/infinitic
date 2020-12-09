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

package io.infinitic.worker.inMemory

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalInput
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalMessageToProcess
import io.infinitic.monitoring.global.engine.worker.startMonitoringGlobalEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameInput
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineInput
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val N_WORKERS = 10

fun CoroutineScope.processTasks(
    keyValueStorage: KeyValueStorage,
    taskCommandsChannel: Channel<TaskEngineMessageToProcess>,
    workflowResultChannel: Channel<WorkflowEngineMessageToProcess>
) = launch(Dispatchers.IO) {

    val taskEventsChannel = Channel<TaskEngineMessageToProcess>()
    val taskResultsChannel = Channel<TaskEngineMessageToProcess>()
    val executorChannel = Channel<TaskExecutorMessageToProcess>()
    val executorResultsChannel = Channel<TaskExecutorMessageToProcess>()
    val monitoringPerNameChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringPerNameResultsChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringGlobalChannel = Channel<MonitoringGlobalMessageToProcess>()
    val monitoringGlobalResultsChannel = Channel<MonitoringGlobalMessageToProcess>()

    // Starting Monitoring Global Engine

    launch {
        for (result in monitoringGlobalResultsChannel) {
            // no message acknowledging for inMemory implementation
            if (result.exception != null) {
                println(result.exception)
            }
        }
    }

    startMonitoringGlobalEngine(
        "monitoring-global-engine",
        MonitoringGlobalStateKeyValueStorage(keyValueStorage),
        MonitoringGlobalInput(
            monitoringGlobalChannel,
            monitoringGlobalResultsChannel
        )
    )

    // Starting Monitoring Per Name Engine

    launch {
        for (result in monitoringPerNameResultsChannel) {
            // no message acknowledging for inMemory implementation
            if (result.exception != null) {
                println(result.exception)
            }
        }
    }

    startMonitoringPerNameEngine(
        "monitoring-per-name-engine",
        MonitoringPerNameStateKeyValueStorage(keyValueStorage),
        MonitoringPerNameInput(
            monitoringPerNameChannel,
            monitoringPerNameResultsChannel
        ),
        InMemoryMonitoringPerNameOutput(
            this,
            monitoringGlobalChannel
        )
    )

    // Starting Task Executors

    launch {
        for (result in executorResultsChannel) {
            // no message acknowledging for inMemory implementation
            if (result.exception != null) {
                println(result.exception)
            }
        }
    }

    repeat(N_WORKERS) {
        startTaskExecutor(
            "task-executor-$it",
            TaskExecutorInput(
                executorChannel,
                executorResultsChannel
            ),
            InMemoryTaskExecutorOutput(
                this,
                taskEventsChannel
            )
        )
    }

    launch {
        for (result in taskResultsChannel) {
            // no message acknowledging for inMemory implementation
            if (result.exception != null) {
                println(result.exception)
            }
        }
    }

    // Starting Task Engine

    startTaskEngine(
        "task-engine",
        TaskStateKeyValueStorage(keyValueStorage),
        NoTaskEventStorage(),
        TaskEngineInput(
            taskCommandsChannel,
            taskEventsChannel,
            taskResultsChannel
        ),
        InMemoryTaskEngineOutput(
            this,
            taskEventsChannel,
            executorChannel,
            monitoringPerNameChannel,
            workflowResultChannel
        )
    )
}
