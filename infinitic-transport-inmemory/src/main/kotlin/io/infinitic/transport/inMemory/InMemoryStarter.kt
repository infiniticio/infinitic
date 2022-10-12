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

import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.ClientStarter
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.tasks.executor.TaskExecutor
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

class InMemoryStarter(private val scope: CoroutineScope, name: String) : ClientStarter, WorkerStarter {

    private val logger = KotlinLogging.logger {}

    private val clientName = ClientName(name)

    // Client channel
    private val clientChannel = Channel<ClientMessage>()

    // Task channels
    private val taskTagChannels = ConcurrentHashMap<ServiceName, Channel<TaskTagMessage>>()
    private val taskExecutorChannels = ConcurrentHashMap<ServiceName, Channel<TaskExecutorMessage>>()

    // Workflow channels
    private val workflowTagChannels = ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessage>>()
    private val workflowEngineChannels = ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()
    private val workflowTaskExecutorChannels = ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessage>>()

    override fun CoroutineScope.startWorkflowTag(
        workflowName: WorkflowName,
        workflowTagStorage: WorkflowTagStorage,
        concurrency: Int
    ) {
        val workflowTagEngine = WorkflowTagEngine(
            clientName,
            workflowTagStorage,
            sendToWorkflowTagAsync,
            sendToWorkflowEngine,
            sendToClientAsync
        )

        startAsync(
            { message: WorkflowTagMessage -> workflowTagEngine.handle(message) },
            getWorkflowTagChannel(workflowName)
        )
    }

    override fun CoroutineScope.startWorkflowEngine(
        workflowName: WorkflowName,
        workflowStateStorage: WorkflowStateStorage,
        concurrency: Int
    ) {
        val workflowEngine = WorkflowEngine(
            clientName,
            workflowStateStorage,
            sendToClientAsync,
            sendToTaskTagAsync,
            sendToTaskExecutor,
            sendToWorkflowTaskExecutor(workflowName),
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

    override fun CoroutineScope.startTaskTag(
        serviceName: ServiceName,
        taskTagStorage: TaskTagStorage,
        concurrency: Int
    ) {
        val taskTagEngine = TaskTagEngine(
            clientName,
            taskTagStorage,
            sendToClientAsync
        )

        startAsync(
            { message: TaskTagMessage -> taskTagEngine.handle(message) },
            getTaskTagChannel(serviceName)
        )
    }

    override fun CoroutineScope.startTaskExecutor(
        serviceName: ServiceName,
        concurrency: Int,
        workerRegistry: WorkerRegistry,
        clientFactory: ClientFactory
    ) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegistry,
            sendToTaskExecutorAfter,
            sendToTaskTag,
            sendToWorkflowEngineAsync,
            sendToClientAsync,
            clientFactory
        )

        repeat(concurrency) {
            startAsync(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                getTaskExecutorChannel(serviceName)
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskExecutor(
        workflowName: WorkflowName,
        concurrency: Int,
        workerRegistry: WorkerRegistry,
        clientFactory: ClientFactory
    ) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegistry,
            sendToWorkflowTaskExecutorAfterAsync(workflowName),
            {}, // workflow tasks do not have tags
            sendToWorkflowEngineAsync,
            sendToClientAsync,
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

    override fun CoroutineScope.startClientResponse(client: InfiniticClientInterface) {
        startAsync(
            { message: ClientMessage -> client.handle(message) },
            clientChannel
        )
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage ->
        scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName)).join()
    }

    override val sendToWorkflowTag: SendToWorkflowTag = { message: WorkflowTagMessage ->
        scope.sendAsync(message, getWorkflowTagChannel(message.workflowName)).join()
    }

    private val sendToTaskTag: SendToTaskTag = { message: TaskTagMessage ->
        scope.sendAsync(message, getTaskTagChannel(message.serviceName)).join()
    }

    private val sendToTaskExecutor: SendToTaskExecutor = { message: TaskExecutorMessage ->
        scope.sendAsync(message, getTaskExecutorChannel(message.serviceName)).join()
    }

    private val sendToTaskTagAsync: SendToTaskTag = { message: TaskTagMessage ->
        scope.sendAsync(message, getTaskTagChannel(message.serviceName))
    }

    private val sendToWorkflowTagAsync: SendToWorkflowTag = { message: WorkflowTagMessage ->
        scope.sendAsync(message, getWorkflowTagChannel(message.workflowName))
    }

    private val sendToWorkflowEngineAsync: SendToWorkflowEngine = { message: WorkflowEngineMessage ->
        scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName))
    }

    private val sendToClientAsync: SendToClient = { message: ClientMessage ->
        scope.sendAsync(message, clientChannel)
    }

    private val sendToTaskExecutorAfter: SendToTaskExecutorAfter =
        { message: TaskExecutorMessage, after: MillisDuration ->
            scope.sendAsync(message, getTaskExecutorChannel(message.serviceName), after).join()
        }

    private val sendToWorkflowEngineAfterAsync: SendToWorkflowEngineAfter =
        { message: WorkflowEngineMessage, after: MillisDuration ->
            scope.sendAsync(message, getWorkflowEngineChannel(message.workflowName), after)
        }

    private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
        val channel = getWorkflowTaskExecutorChannel(workflowName)

        return { message: TaskExecutorMessage -> scope.sendAsync(message, channel).join() }
    }

    private fun sendToWorkflowTaskExecutorAfterAsync(workflowName: WorkflowName): SendToTaskExecutorAfter {
        val channel = getWorkflowTaskExecutorChannel(workflowName)

        return { message: TaskExecutorMessage, after: MillisDuration -> scope.sendAsync(message, channel, after) }
    }

    private fun getTaskTagChannel(serviceName: ServiceName) =
        taskTagChannels[serviceName] ?: run {
            val channel = Channel<TaskTagMessage>()
            taskTagChannels[serviceName] = channel
            channel
        }

    private fun getTaskExecutorChannel(serviceName: ServiceName) =
        taskExecutorChannels[serviceName] ?: run {
            val channel = Channel<TaskExecutorMessage>()
            taskExecutorChannels[serviceName] = channel
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

    private fun getWorkflowTaskExecutorChannel(workflowName: WorkflowName) =
        workflowTaskExecutorChannels[workflowName] ?: run {
            val channel = Channel<TaskExecutorMessage>()
            workflowTaskExecutorChannels[workflowName] = channel
            channel
        }

    private fun <T : Message> CoroutineScope.sendAsync(
        message: T,
        channel: Channel<T>,
        after: MillisDuration = MillisDuration.ZERO
    ): CompletableFuture<Unit> = future {
        logger.debug {
            val prefix = if (after > 0) "after $after ms, " else ""
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
