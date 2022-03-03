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

package io.infinitic.transport.inMemory

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.ClientMessageToProcess
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.metrics.global.SendToGlobalMetrics
import io.infinitic.common.metrics.global.messages.GlobalMetricsMessage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.metrics.SendToTaskMetrics
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.metrics.global.engine.worker.MetricsGlobalMessageToProcess
import io.infinitic.metrics.perName.engine.worker.MetricsPerNameMessageToProcess
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

typealias WorkflowTagMessageToProcess = MessageToProcess<WorkflowTagMessage>
typealias TaskTagMessageToProcess = MessageToProcess<TaskTagMessage>

class InMemoryOutput(private val scope: CoroutineScope) {
    private val logger = KotlinLogging.logger {}

    val logChannel: Channel<MessageToProcess<Any>> = Channel()
    val clientChannel: Channel<ClientMessageToProcess> = Channel()

    val taskTagChannel: ConcurrentHashMap<TaskName, Channel<TaskTagMessageToProcess>> = ConcurrentHashMap()
    val taskChannel: ConcurrentHashMap<TaskName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
    val taskExecutorChannel: ConcurrentHashMap<TaskName, Channel<TaskExecutorMessageToProcess>> = ConcurrentHashMap()
    val taskMetricsPerNameChannel: ConcurrentHashMap<TaskName, Channel<MetricsPerNameMessageToProcess>> = ConcurrentHashMap()

    val workflowTagChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessageToProcess>> = ConcurrentHashMap()
    val workflowChannel: ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessageToProcess>> = ConcurrentHashMap()
    val workflowTaskChannel: ConcurrentHashMap<WorkflowName, Channel<TaskEngineMessageToProcess>> = ConcurrentHashMap()
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

    val sendCommandsToTaskTag: SendToTaskTag = { message: TaskTagMessage ->
        logger.debug { "sendCommandsToTaskTag $message" }
        scope.future {
            taskTagChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToTaskTag: SendToTaskTag = { message: TaskTagMessage ->
        logger.debug { "sendEventsToTaskTag $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            taskTagChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    fun sendCommandsToTaskEngine(name: Name? = null): SendToTaskEngine = when (name) {
        null -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                taskChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is TaskName -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                taskChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is WorkflowName -> { message ->
            logger.debug { "sendCommandsToTaskEngine $message" }
            scope.future {
                workflowTaskChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        else -> thisShouldNotHappen()
    }

    fun sendEventsToTaskEngine(name: Name): SendToTaskEngine = when (name) {
        is TaskName -> { message ->
            logger.debug { "sendEventsToTaskEngine $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                taskChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        is WorkflowName -> { message ->
            logger.debug { "sendEventsToTaskEngine $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                workflowTaskChannel[name]!!.send(InMemoryMessageToProcess(message))
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
                taskChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        is WorkflowName -> { message, after ->
            logger.debug { "sendToTaskEngineAfter $message" }
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            scope.future {
                delay(after.long)
                workflowTaskChannel[name]!!.send(InMemoryMessageToProcess(message))
            }
        }
        else -> thisShouldNotHappen()
    }

    val sendToWorkflowTaskEngine = { workflowTask: WorkflowTaskParameters ->
        logger.debug { "sendToWorkflowTaskEngine $workflowTask" }
        scope.future {
            workflowTaskChannel[workflowTask.workflowName]!!.send(
                InMemoryMessageToProcess(
                    workflowTask.toDispatchTaskMessage()
                )
            )
        }

        Unit
    }

    fun sendToTaskExecutors(name: Name): SendToTaskExecutor = when (name) {
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

    val sendCommandsToWorkflowTagEngine: SendToWorkflowTag = { message ->
        logger.debug { "sendCommandsToWorkflowTagEngine $message" }
        scope.future {
            workflowTagChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToWorkflowTagEngine: SendToWorkflowTag = { message ->
        logger.debug { "sendEventsToWorkflowTagEngine $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            workflowTagChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    val sendCommandsToWorkflowEngine: SendToWorkflowEngine = { message ->
        logger.debug { "sendCommandsToWorkflowEngine $message" }
        scope.future {
            workflowChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }.join()
    }

    val sendEventsToWorkflowEngine: SendToWorkflowEngine = { message ->
        logger.debug { "sendEventsToWorkflowEngine $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.future {
            workflowChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = { message, after ->
        logger.debug { "sendToWorkflowEngineAfter $message" }
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            delay(after.long)
            workflowChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
        }
    }

    fun sendToMetricsPerName(name: Name): SendToTaskMetrics = when (name) {
        is TaskName -> { message: TaskMetricsMessage ->
            logger.debug { "sendToMonitoringPerName $message" }
            scope.future {
                taskMetricsPerNameChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        is WorkflowName -> { message: TaskMetricsMessage ->
            logger.debug { "sendToMonitoringPerName $message" }
            scope.future {
//                workflowMetricsPerNameChannel[name]!!.send(InMemoryMessageToProcess(message))
            }.join()
        }
        else -> thisShouldNotHappen()
    }

    val sendToGlobalMetrics: SendToGlobalMetrics = { message: GlobalMetricsMessage ->
        logger.debug { "sendToMonitoringGlobal $message" }
        scope.future {
            metricsGlobalChannel.send(InMemoryMessageToProcess(message))
        }.join()
    }
}
