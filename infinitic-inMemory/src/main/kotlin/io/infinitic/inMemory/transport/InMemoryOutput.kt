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
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.metrics.global.engine.worker.MetricsGlobalMessageToProcess
import io.infinitic.metrics.perName.engine.worker.MetricsPerNameMessageToProcess
import io.infinitic.tags.tasks.worker.TaskTagEngineMessageToProcess
import io.infinitic.tags.workflows.worker.WorkflowTagEngineMessageToProcess
import io.infinitic.tasks.engine.worker.TaskEngineMessageToProcess
import io.infinitic.tasks.executor.worker.TaskExecutorMessageToProcess
import io.infinitic.workflows.engine.worker.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InMemoryOutput(
    private val scope: CoroutineScope,
    val logChannel: Channel<MessageToProcess<Any>> = Channel(),
    val clientEventsChannel: Channel<ClientMessageToProcess> = Channel(),
    val taskTagCommandsChannel: Channel<TaskTagEngineMessageToProcess> = Channel(),
    val taskTagEventsChannel: Channel<TaskTagEngineMessageToProcess> = Channel(),
    val taskCommandsChannel: Channel<TaskEngineMessageToProcess> = Channel(),
    val taskEventsChannel: Channel<TaskEngineMessageToProcess> = Channel(),
    val workflowTagCommandsChannel: Channel<WorkflowTagEngineMessageToProcess> = Channel(),
    val workflowTagEventsChannel: Channel<WorkflowTagEngineMessageToProcess> = Channel(),
    val workflowCommandsChannel: Channel<WorkflowEngineMessageToProcess> = Channel(),
    val workflowEventsChannel: Channel<WorkflowEngineMessageToProcess> = Channel(),
    val executorChannel: Channel<TaskExecutorMessageToProcess> = Channel(),
    val metricsPerNameChannel: Channel<MetricsPerNameMessageToProcess> = Channel(),
    val metricsGlobalChannel: Channel<MetricsGlobalMessageToProcess> = Channel()
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    val sendEventsToClient: SendToClient = { message: ClientMessage ->
        logger.debug("sendEventsToClient {}", message)
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            clientEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToTaskTagEngine: SendToTaskTagEngine = { message: TaskTagEngineMessage ->
        logger.debug("sendCommandsToTaskTagEngine {}", message)
        taskTagCommandsChannel.send(InMemoryMessageToProcess(message))
    }

    val sendEventsToTaskTagEngine: SendToTaskTagEngine = { message: TaskTagEngineMessage ->
        logger.debug("sendEventsToTaskTagEngine {}", message)
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            taskTagEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: MillisDuration ->
        logger.debug("sendCommandsToTaskEngine {}", message)
        delay(after.long)
        taskCommandsChannel.send(InMemoryMessageToProcess(message))
    }

    val sendEventsToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: MillisDuration ->
        logger.debug("sendEventsToTaskEngine {}", message)
        delay(after.long)
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            taskEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToWorkflowTagEngine: SendToWorkflowTagEngine = { message: WorkflowTagEngineMessage ->
        logger.debug("sendCommandsToWorkflowTagEngine {}", message)
        workflowTagCommandsChannel.send(InMemoryMessageToProcess(message))
    }

    val sendEventsToWorkflowTagEngine: SendToWorkflowTagEngine = { message: WorkflowTagEngineMessage ->
        logger.debug("sendEventsToWorkflowTagEngine {}", message)
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            workflowTagEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: MillisDuration ->
        logger.debug("sendCommandsToWorkflowEngine {}", message)
        delay(after.long)
        workflowCommandsChannel.send(InMemoryMessageToProcess(message))
    }

    val sendEventsToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: MillisDuration ->
        logger.debug("sendEventsToWorkflowEngine {}", message)
        delay(after.long)
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            workflowEventsChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendToTaskExecutors: SendToTaskExecutors = { message: TaskExecutorMessage ->
        logger.debug("sendToTaskExecutors {}", message)
        executorChannel.send(InMemoryMessageToProcess(message))
    }

    val sendToMetricsPerName: SendToMetricsPerName = { message: MetricsPerNameMessage ->
        logger.debug("sendToMonitoringPerName {}", message)
        metricsPerNameChannel.send(InMemoryMessageToProcess(message))
    }

    val sendToMetricsGlobal: SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        logger.debug("sendToMonitoringGlobal {}", message)
        metricsGlobalChannel.send(InMemoryMessageToProcess(message))
    }
}
