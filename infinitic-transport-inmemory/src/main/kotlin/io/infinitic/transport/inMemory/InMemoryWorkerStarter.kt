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

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.InfiniticClient
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engines.SendToTaskEngine
import io.infinitic.common.tasks.engines.SendToTaskEngineAfter
import io.infinitic.common.tasks.engines.messages.TaskEngineMessage
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.metrics.SendToTaskMetrics
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.WorkerRegister
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.metrics.TaskMetricsEngine
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class InMemoryWorkerStarter(private val scope: CoroutineScope, name: String) : WorkerStarter {

    private val logger = KotlinLogging.logger {}

    private val clientName = ClientName(name)

    // Client channel
    private val clientChannel = Channel<ClientMessage>()

    // Task channels
    private val taskTagChannels = ConcurrentHashMap<TaskName, Channel<TaskTagMessage>>()
    private val taskEngineChannels = ConcurrentHashMap<TaskName, Channel<TaskEngineMessage>>()
    private val taskMetricsChannels = ConcurrentHashMap<TaskName, Channel<TaskMetricsMessage>>()
    private val taskExecutorChannels = ConcurrentHashMap<TaskName, Channel<TaskExecutorMessage>>()

    // Workflow channels
    private val workflowTagChannels = ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessage>>()
    private val workflowEngineChannels = ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()
    private val workflowTaskEngineChannels = ConcurrentHashMap<WorkflowName, Channel<TaskEngineMessage>>()
    private val workflowTaskMetricsChannels = ConcurrentHashMap<WorkflowName, Channel<TaskMetricsMessage>>()
    private val workflowTaskExecutorChannels = ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessage>>()

    override fun CoroutineScope.startClientResponse(client: InfiniticClient) {
        startAsync(
            { message: ClientMessage -> client.handle(message) },
            clientChannel
        )
    }

    override fun CoroutineScope.startWorkflowTag(workflowName: WorkflowName, workflowTagStorage: WorkflowTagStorage, concurrency: Int) {
        val workflowTagEngine = WorkflowTagEngine(
            clientName,
            workflowTagStorage,
            sendToWorkflowEngine,
            sendToClientAsync
        )

        startAsync(
            { message: WorkflowTagMessage -> workflowTagEngine.handle(message) },
            getWorkflowTagChannel(workflowName)
        )
    }

    override fun CoroutineScope.startWorkflowEngine(workflowName: WorkflowName, workflowStateStorage: WorkflowStateStorage, concurrency: Int) {
        val workflowEngine = WorkflowEngine(
            clientName,
            workflowStateStorage,
            sendToClientAsync,
            sendToTaskTagAsync,
            sendToTaskEngine,
            sendToWorkflowTaskEngine(workflowName),
            sendToWorkflowTagAsync,
            sendToWorkflowEngineAsync,
            sendToWorkflowEngineAfterAsync
        )

        startAsync(
            { message: WorkflowEngineMessage -> workflowEngine.handle(message) },
            getWorkflowEngineChannel(workflowName)
        )
    }

    override fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int) {
        // not needed
    }

    override fun CoroutineScope.startTaskTag(taskName: TaskName, taskTagStorage: TaskTagStorage, concurrency: Int) {
        val taskTagEngine = TaskTagEngine(
            clientName,
            taskTagStorage,
            sendToTaskEngine,
            sendToClientAsync
        )

        startAsync(
            { message: TaskTagMessage -> taskTagEngine.handle(message) },
            getTaskTagChannel(taskName)
        )
    }

    override fun CoroutineScope.startTaskEngine(taskName: TaskName, taskStateStorage: TaskStateStorage, concurrency: Int) {
        val taskEngine = TaskEngine(
            clientName,
            taskStateStorage,
            sendToClientAsync,
            sendToTaskTagAsync,
            sendToTaskEngineAfterAsync,
            sendToWorkflowEngineAsync,
            sendToTaskExecutor,
            sendToTaskMetrics,
        )

        startAsync(
            { message: TaskEngineMessage -> taskEngine.handle(message) },
            getTaskEngineChannel(taskName)
        )
    }

    override fun CoroutineScope.startTaskMetrics(taskName: TaskName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        startAsync(
            { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
            getTaskMetricsChannel(taskName)
        )
    }

    override fun CoroutineScope.startTaskDelay(taskName: TaskName, concurrency: Int) {
        // not needed
    }

    override fun CoroutineScope.startTaskExecutor(taskName: TaskName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToTaskEngineAsync,
            clientFactory
        )

        repeat(concurrency) {
            startAsync(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                getTaskExecutorChannel(taskName)
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskEngine(workflowName: WorkflowName, taskStateStorage: TaskStateStorage, concurrency: Int) {
        val taskEngine = TaskEngine(
            clientName,
            taskStateStorage,
            sendToClientAsync,
            sendToTaskTagAsync,
            sendToWorkflowTaskEngineAfterAsync(workflowName),
            sendToWorkflowEngineAsync,
            sendToWorkflowTaskExecutor(workflowName),
            sendToWorkflowTaskMetrics(workflowName),
        )

        startAsync(
            { message: TaskEngineMessage -> taskEngine.handle(message) },
            getWorkflowTaskEngineChannel(workflowName)
        )
    }

    override fun CoroutineScope.startWorkflowTaskMetrics(workflowName: WorkflowName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        startAsync(
            { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
            getWorkflowTaskMetricsChannel(workflowName)
        )
    }

    override fun CoroutineScope.startWorkflowTaskDelay(workflowName: WorkflowName, concurrency: Int) {
        // not needed
    }

    override fun CoroutineScope.startWorkflowTaskExecutor(workflowName: WorkflowName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToWorkflowTaskEngineAsync(workflowName),
            clientFactory
        )
        val channel = getWorkflowTaskExecutorChannel(workflowName)

        repeat(concurrency) {
            startAsync(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                channel
            )
        }
    }

    override val sendToTaskTag: SendToTaskTag = { message: TaskTagMessage ->
        scope.sendAsync(message, getTaskTagChannel(message.taskName)).join()
    }

    private val sendToTaskTagAsync: SendToTaskTag = { message: TaskTagMessage ->
        scope.sendAsync(message, getTaskTagChannel(message.taskName))
    }

    override val sendToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage ->
        scope.sendAsync(message, getTaskEngineChannel(message.taskName)).join()
    }

    private val sendToTaskEngineAsync: SendToTaskEngine = { message: TaskEngineMessage ->
        scope.sendAsync(message, getTaskEngineChannel(message.taskName))
    }

    override val sendToWorkflowTag: SendToWorkflowTag = { message: WorkflowTagMessage ->
        scope.sendAsync(message, getWorkflowTagChannel(message.workflowName)).join()
    }

    private val sendToWorkflowTagAsync: SendToWorkflowTag = { message: WorkflowTagMessage ->
        scope.sendAsync(message, getWorkflowTagChannel(message.workflowName))
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage ->
        scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName)).join()
    }

    private val sendToWorkflowEngineAsync: SendToWorkflowEngine = { message: WorkflowEngineMessage ->
        scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName))
    }

    private val sendToClientAsync: SendToClient = { message: ClientMessage ->
        scope.sendAsync(message, clientChannel)
    }

    private val sendToTaskEngineAfterAsync: SendToTaskEngineAfter =
        { message: TaskEngineMessage, after: MillisDuration ->
            scope.sendAsync(message, getTaskEngineChannel(message.taskName), after)
        }

    private val sendToTaskExecutor: SendToTaskExecutor = { message: TaskExecutorMessage ->
        scope.sendAsync(message, getTaskExecutorChannel(message.taskName)).join()
    }

    private val sendToTaskMetrics: SendToTaskMetrics = { message: TaskMetricsMessage ->
        scope.sendAsync(message, getTaskMetricsChannel(message.taskName)).join()
    }

    private val sendToWorkflowEngineAfterAsync: SendToWorkflowEngineAfter =
        { message: WorkflowEngineMessage, after: MillisDuration ->
            scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName), after)
        }

    private fun sendToWorkflowTaskEngine(workflowName: WorkflowName): SendToTaskEngine {
        val channel = getWorkflowTaskEngineChannel(workflowName)

        return { message: TaskEngineMessage -> scope.sendAsync(message, channel).join() }
    }

    private fun sendToWorkflowTaskEngineAsync(workflowName: WorkflowName): SendToTaskEngine {
        val channel = getWorkflowTaskEngineChannel(workflowName)

        return { message: TaskEngineMessage -> scope.sendAsync(message, channel) }
    }

    private fun sendToWorkflowTaskEngineAfterAsync(workflowName: WorkflowName): SendToTaskEngineAfter {
        val channel = getWorkflowTaskEngineChannel(workflowName)

        return { message: TaskEngineMessage, after: MillisDuration -> scope.sendAsync(message, channel, after) }
    }

    private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
        val channel = getWorkflowTaskExecutorChannel(workflowName)

        return { message: TaskExecutorMessage -> scope.sendAsync(message, channel).join() }
    }

    private fun sendToWorkflowTaskMetrics(workflowName: WorkflowName): SendToTaskMetrics {
        val channel = getWorkflowTaskMetricsChannel(workflowName)

        return { message: TaskMetricsMessage -> scope.sendAsync(message, channel).join() }
    }

    private fun getTaskTagChannel(taskName: TaskName) =
        taskTagChannels[taskName] ?: run {
            val channel = Channel<TaskTagMessage>()
            taskTagChannels[taskName] = channel
            channel
        }

    private fun getTaskEngineChannel(taskName: TaskName) =
        taskEngineChannels[taskName] ?: run {
            val channel = Channel<TaskEngineMessage>()
            taskEngineChannels[taskName] = channel
            channel
        }

    private fun getTaskExecutorChannel(taskName: TaskName) =
        taskExecutorChannels[taskName] ?: run {
            val channel = Channel<TaskExecutorMessage>()
            taskExecutorChannels[taskName] = channel
            channel
        }

    private fun getTaskMetricsChannel(taskName: TaskName) =
        taskMetricsChannels[taskName] ?: run {
            val channel = Channel<TaskMetricsMessage>()
            taskMetricsChannels[taskName] = channel
            channel
        }

    private fun getWorkflowTagChannel(workflowName: WorkflowName) =
        workflowTagChannels[workflowName] ?: run {
            val channel = Channel<WorkflowTagMessage>()
            workflowTagChannels[workflowName] = channel
            channel
        }

    private fun getWorkflowEngineChannel(workflowName: WorkflowName) =
        workflowEngineChannels[workflowName] ?: run {
            val channel = Channel<WorkflowEngineMessage>()
            workflowEngineChannels[workflowName] = channel
            channel
        }

    private fun getWorkflowTaskEngineChannel(workflowName: WorkflowName) =
        workflowTaskEngineChannels[workflowName] ?: run {
            val channel = Channel<TaskEngineMessage>()
            workflowTaskEngineChannels[workflowName] = channel
            channel
        }

    private fun getWorkflowTaskExecutorChannel(workflowName: WorkflowName) =
        workflowTaskExecutorChannels[workflowName] ?: run {
            val channel = Channel<TaskExecutorMessage>()
            workflowTaskExecutorChannels[workflowName] = channel
            channel
        }

    private fun getWorkflowTaskMetricsChannel(workflowName: WorkflowName) =
        workflowTaskMetricsChannels[workflowName] ?: run {
            val channel = Channel<TaskMetricsMessage>()
            workflowTaskMetricsChannels[workflowName] = channel
            channel
        }

    private fun <T : Message> CoroutineScope.sendAsync(
        message: T,
        channel: Channel<T>,
        after: MillisDuration = MillisDuration.ZERO
    ): CompletableFuture<Unit> = future {
        logger.debug {
            val prefix = if (after> 0) "after $after ms, " else ""
            "${prefix}sending $message"
        }
        if (after > 0) delay(after.long)
        channel.send(message)
        logger.debug { "sent" }
    }

    private fun <T : Message> CoroutineScope.startAsync(
        executor: suspend (T) -> Unit,
        channel: Channel<T>
    ): CompletableFuture<Unit> = future {
        for (message in channel) {
            try {
                executor(message)
            } catch (e: Throwable) {
                logger.error(e) { "Error while processing message $message" }
            }
        }
    }
}
