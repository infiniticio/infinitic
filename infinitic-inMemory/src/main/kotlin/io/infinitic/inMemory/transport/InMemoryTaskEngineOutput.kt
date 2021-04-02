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

package io.infinitic.inMemory.transport

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.ClientMessageToProcess
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.perName.engine.input.MonitoringPerNameMessageToProcess
import io.infinitic.tags.engine.input.TagEngineMessageToProcess
import io.infinitic.tasks.engine.input.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.output.TaskEngineOutput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.workflows.engine.input.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InMemoryTaskEngineOutput(
    private val scope: CoroutineScope,
    private val clientResponsesChannel: SendChannel<ClientMessageToProcess>,
    private val tagEventsChannel: SendChannel<TagEngineMessageToProcess>,
    private val taskEventsChannel: SendChannel<TaskEngineMessageToProcess>,
    private val executorChannel: SendChannel<TaskExecutorMessageToProcess>,
    private val monitoringPerNameChannel: SendChannel<MonitoringPerNameMessageToProcess>,
    private val workflowEventsChannel: SendChannel<WorkflowEngineMessageToProcess>
) : TaskEngineOutput {

    override suspend fun sendToClientResponse(message: ClientMessage) {
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            clientResponsesChannel.send(InMemoryMessageToProcess(message))
        }
    }

    override suspend fun sendToTagEngine(message: TagEngineMessage) {
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            tagEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    override suspend fun sendToTaskEngine(message: TaskEngineMessage, after: MillisDuration) {
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            delay(after.long)
            taskEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    override suspend fun sendToWorkflowEngine(message: WorkflowEngineMessage) {
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            workflowEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    override suspend fun sendToTaskExecutors(message: TaskExecutorMessage) {
        executorChannel.send(InMemoryMessageToProcess(message))
    }

    override suspend fun sendToMonitoringPerName(message: MetricsPerNameMessage) {
        monitoringPerNameChannel.send(InMemoryMessageToProcess(message))
    }
}
