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
import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
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
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutput(private val scope: CoroutineScope) {
    private val logger = KotlinLogging.logger {}

    val logChannel: Channel<MessageToProcess<Any>> = Channel()
    val clientChannel: Channel<ClientMessageToProcess> = Channel()

    val taskTagCommandsChannel: ConcurrentHashMap<TaskName, Channel<TaskTagEngineMessageToProcess>> = ConcurrentHashMap()
    val taskTagEventsChannel: ConcurrentHashMap<TaskName, Channel<TaskTagEngineMessageToProcess>> = ConcurrentHashMap()
    val taskCommandsChannel: ConcurrentHashMap<TaskName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
    val taskEventsChannel: ConcurrentHashMap<TaskName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
    val taskExecutorChannel: ConcurrentHashMap<TaskName, Channel<TaskExecutorMessageToProcess>> = ConcurrentHashMap()
    val taskMetricsPerNameChannel: ConcurrentHashMap<TaskName, Channel<MetricsPerNameMessageToProcess>> = ConcurrentHashMap()

    val workflowTagCommandsChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowTagEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowTagEventsChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowTagEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowCommandsChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowEventsChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowTaskCommandsChannel: ConcurrentHashMap<WorkflowName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowTaskEventsChannel: ConcurrentHashMap<WorkflowName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowTaskExecutorChannel: ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessageToProcess>> = ConcurrentHashMap()
    val workflowMetricsPerNameChannel: ConcurrentHashMap<WorkflowName, Channel<MetricsPerNameMessageToProcess>> = ConcurrentHashMap()

    val metricsGlobalChannel: Channel<MetricsGlobalMessageToProcess> = Channel()

    val sendToClient: SendToClient = { message: ClientMessage ->
        logger.debug { "sendToClient $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            clientChannel.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToTaskTagEngine: SendToTaskTagEngine = { message: TaskTagEngineMessage ->
        logger.debug { "sendCommandsToTaskTagEngine $message" }
        scope.future {
            taskTagCommandsChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToTaskTagEngine: SendToTaskTagEngine = { message: TaskTagEngineMessage ->
        logger.debug { "sendEventsToTaskTagEngine $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            taskTagEventsChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    fun sendCommandsToTaskEngine(name: Name? = null): SendToTaskEngine = when (name) {
        null -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                taskCommandsChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is TaskName -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                taskCommandsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is WorkflowName -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                workflowTaskCommandsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        else -> thisShouldNotHappen()
    }

    fun sendEventsToTaskEngine(name: Name): SendToTaskEngine = when (name) {
        is TaskName -> { message ->
            logger.debug { "sendEventsToTaskEngine $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                taskEventsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        is WorkflowName -> { message ->
            logger.debug { "sendEventsToTaskEngine $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                workflowTaskEventsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        else -> thisShouldNotHappen()
    }

    fun sendToTaskEngineAfter(name: Name): SendToTaskEngineAfter = when (name) {
        is TaskName -> { message, after ->
            logger.debug { "sendToTaskEngineAfter $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                delay(after.long)
                taskEventsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        is WorkflowName -> { message, after ->
            logger.debug { "sendToTaskEngineAfter $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                delay(after.long)
                workflowTaskEventsChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        else -> thisShouldNotHappen()
    }

    fun sendToTaskExecutors(name: Name): SendToTaskExecutors = when (name) {
        is TaskName ->
            { message: TaskExecutorMessage ->
                logger.debug { "sendToTaskExecutors $message" }
                scope.future {
                    taskExecutorChannel[name]!!.send(InMemoryMessageToProcess(message))
                }.join()
            }
        is WorkflowName -> {
            { message: TaskExecutorMessage ->
                logger.debug { "sendToWorkflowTaskExecutors $message" }
                scope.future {
                    workflowTaskExecutorChannel[name]!!.send(InMemoryMessageToProcess(message))
                }.join()
            }
        }
        else -> thisShouldNotHappen()
    }

    val sendCommandsToWorkflowTagEngine: SendToWorkflowTagEngine = { message ->
        logger.debug { "sendCommandsToWorkflowTagEngine $message" }
        scope.future {
            workflowTagCommandsChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToWorkflowTagEngine: SendToWorkflowTagEngine = { message ->
        logger.debug { "sendEventsToWorkflowTagEngine $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            workflowTagEventsChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToWorkflowEngine: SendToWorkflowEngine = { message ->
        logger.debug { "sendCommandsToWorkflowEngine $message" }
        scope.future {
            workflowCommandsChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToWorkflowEngine: SendToWorkflowEngine = { message ->
        logger.debug { "sendEventsToWorkflowEngine $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            workflowEventsChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = { message, after ->
        logger.debug { "sendToWorkflowEngineAfter $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            delay(after.long)
            workflowEventsChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    fun sendToMetricsPerName(name: Name): SendToMetricsPerName = when (name) {
        is TaskName -> { message: MetricsPerNameMessage ->
            logger.debug { "sendToMonitoringPerName $message" }
            scope.future {
                taskMetricsPerNameChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is WorkflowName -> { message: MetricsPerNameMessage ->
            logger.debug { "sendToMonitoringPerName $message" }
            scope.future {
//                workflowMetricsPerNameChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        else -> thisShouldNotHappen()
    }

    val sendToMetricsGlobal: SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        logger.debug { "sendToMonitoringGlobal $message" }
        scope.future {
            metricsGlobalChannel.send(InMemoryMessageToProcess(message))
        }.join()
    }
}
