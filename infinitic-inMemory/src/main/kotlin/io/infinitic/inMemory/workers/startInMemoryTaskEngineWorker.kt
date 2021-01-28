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

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.inMemory.transport.InMemoryTaskEngineOutput
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineInputChannels
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

fun CoroutineScope.startInMemoryTaskEngineWorker(
    keyValueStorage: KeyValueStorage,
    taskEngineCommandsChannel: Channel<TaskEngineMessageToProcess>,
    taskEngineEventsChannel: Channel<TaskEngineMessageToProcess>,
    logChannel: SendChannel<TaskEngineMessageToProcess>,
    taskExecutorChannel: Channel<TaskExecutorMessageToProcess>,
    monitoringPerNameChannel: Channel<MonitoringPerNameMessageToProcess>,
    workflowEngineEventsChannel: Channel<WorkflowEngineMessageToProcess>,
) = launch {

    // Starting Task Engine

    startTaskEngine(
        "task-engine",
        TaskStateKeyValueStorage(keyValueStorage),
        NoTaskEventStorage(),
        TaskEngineInputChannels(
            taskEngineCommandsChannel,
            taskEngineEventsChannel,
            logChannel
        ),
        InMemoryTaskEngineOutput(
            this,
            taskEngineEventsChannel,
            taskExecutorChannel,
            monitoringPerNameChannel,
            workflowEngineEventsChannel
        )
    )
}
