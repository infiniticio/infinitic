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

import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToExecutors
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InMemoryTaskEngineOutput(
    scope: CoroutineScope,
    taskEventsChannel: Channel<TaskEngineMessageToProcess>,
    executorChannel: SendChannel<TaskExecutorMessageToProcess>,
    monitoringPerNameChannel: SendChannel<MonitoringPerNameMessageToProcess>,
    workflowEventsChannel: SendChannel<WorkflowEngineMessageToProcess>
) : TaskEngineOutput {
    override val sendToWorkflowEngine: SendToWorkflowEngine = { msg: WorkflowEngineMessage, after: Float ->
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            // TODO inMemory resilience implies to find a way to persist delayed messages
            delay((1000 * after).toLong())
            workflowEventsChannel.send(InMemoryMessageToProcess(msg))
        }
    }

    override val sendToTaskEngine: SendToTaskEngine = { msg: TaskEngineMessage, after: Float ->
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            // TODO inMemory resilience implies to find a way to persist delayed messages
            delay((1000 * after).toLong())
            taskEventsChannel.send(InMemoryMessageToProcess(msg))
        }
    }

    override val sendToExecutors: SendToExecutors = {
        executorChannel.send(InMemoryMessageToProcess(it))
    }

    override val sendToMonitoringPerName: SendToMonitoringPerName = {
        monitoringPerNameChannel.send(InMemoryMessageToProcess(it))
    }
}
