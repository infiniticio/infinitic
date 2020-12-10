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
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalMessageToProcess
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val N_WORKERS = 10

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    keyValueStorage: KeyValueStorage,
    taskEngineCommandsChannel: Channel<TaskEngineMessageToProcess>,
    workflowEngineCommandsChannel: Channel<WorkflowEngineMessageToProcess>
) = launch(Dispatchers.IO) {

    val workflowEngineEventsChannel = Channel<WorkflowEngineMessageToProcess>()
    val workflowEngineResultsChannel = Channel<WorkflowEngineMessageToProcess>()
    val taskEngineEventsChannel = Channel<TaskEngineMessageToProcess>()
    val taskEngineResultsChannel = Channel<TaskEngineMessageToProcess>()
    val taskExecutorChannel = Channel<TaskExecutorMessageToProcess>()
    val taskExecutorResultsChannel = Channel<TaskExecutorMessageToProcess>()
    val monitoringPerNameChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringPerNameResultsChannel = Channel<MonitoringPerNameMessageToProcess>()
    val monitoringGlobalChannel = Channel<MonitoringGlobalMessageToProcess>()
    val monitoringGlobalResultsChannel = Channel<MonitoringGlobalMessageToProcess>()

    startInMemoryMonitoringGlobalWorker(
        keyValueStorage,
        monitoringGlobalChannel,
        monitoringGlobalResultsChannel
    )

    startInMemoryMonitoringPerNameWorker(
        keyValueStorage,
        monitoringPerNameChannel,
        monitoringPerNameResultsChannel,
        monitoringGlobalChannel
    )

    startInMemoryTaskExecutorWorker(
        taskExecutorRegister,
        taskEngineEventsChannel,
        taskExecutorChannel,
        taskExecutorResultsChannel,
        N_WORKERS
    )

    startInMemoryTaskEngineWorker(
        keyValueStorage,
        taskEngineCommandsChannel,
        taskEngineEventsChannel,
        taskEngineResultsChannel,
        taskExecutorChannel,
        monitoringPerNameChannel,
        workflowEngineEventsChannel
    )

    startInMemoryWorkflowEngineWorker(
        keyValueStorage,
        workflowEngineCommandsChannel,
        workflowEngineEventsChannel,
        workflowEngineResultsChannel,
        taskEngineCommandsChannel
    )
}
