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

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.tasks.engine.storage.TaskStateKeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

private const val N_WORKERS = 10

fun CoroutineScope.processTasks(
    keyValueStorage: KeyValueStorage,
    taskClientChannel: ReceiveChannel<TaskEngineMessage>,
    workflowResultChannel: SendChannel<WorkflowEngineMessage>
) = launch {

    val monitoringPerNameChannel = Channel<MonitoringPerNameEngineMessage>()
    val monitoringGlobalChannel = Channel<MonitoringGlobalMessage>()
    val taskResultChannel=  Channel<TaskEngineMessage>()
    val executorChannel = Channel<TaskExecutorMessage>()

    startMonitoringGlobalEngine(
        MonitoringGlobalStateKeyValueStorage(keyValueStorage),
        monitoringGlobalChannel
    )

    startMonitoringPerNameEngine(
        MonitoringPerNameStateKeyValueStorage(keyValueStorage),
        monitoringPerNameChannel,
        monitoringGlobalChannel
    )

    repeat(N_WORKERS) {
        startTaskExecutor(executorChannel, taskResultChannel)
    }

    startTaskEngine(
        TaskStateKeyValueStorage(keyValueStorage),
        taskClientChannel,
        taskResultChannel,
        executorChannel,
        monitoringPerNameChannel,
        workflowResultChannel
    )
}
