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

package io.infinitic.transport.pulsar

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.InfiniticClient
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engines.SendToTaskEngine
import io.infinitic.common.tasks.engines.SendToTaskEngineAfter
import io.infinitic.common.tasks.engines.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engines.messages.TaskEngineMessage
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.metrics.SendToTaskMetrics
import io.infinitic.common.tasks.metrics.messages.TaskMetricsEnvelope
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.WorkerRegister
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.metrics.TaskMetricsEngine
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.transport.pulsar.topics.ClientTopics
import io.infinitic.transport.pulsar.topics.TaskTopics
import io.infinitic.transport.pulsar.topics.TopicNames
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import kotlinx.coroutines.CoroutineScope
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType

class PulsarWorkerStarter(private val topicNames: TopicNames, val client: PulsarClient, val name: String) : WorkerStarter {
    private val zero = MillisDuration(0L)
    private val clientName = ClientName(name)
    private val sender = Sender(client)
    private val listener = Listener(client)

    override fun CoroutineScope.startClientResponse(client: InfiniticClient) {
        with(listener) {
            val topicType = ClientTopics.RESPONSE
            start<ClientMessage, ClientEnvelope>(
                { message: ClientMessage -> client.handle(message) },
                topicNames.topic(topicType, ClientName(client.name)),
                SubscriptionType.Exclusive,
                topicType.consumerName(ClientName(client.name))
            )
        }
    }

    override fun CoroutineScope.startWorkflowTag(workflowName: WorkflowName, workflowTagStorage: WorkflowTagStorage, concurrency: Int) {
        val tagEngine = WorkflowTagEngine(
            clientName,
            workflowTagStorage,
            sendToWorkflowEngine,
            sendToClient
        )

        with(listener) {
            val topicType = WorkflowTopics.TAG
            start<WorkflowTagMessage, WorkflowTagEnvelope>(
                { message: WorkflowTagMessage -> tagEngine.handle(message) },
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowEngine(workflowName: WorkflowName, workflowStateStorage: WorkflowStateStorage, concurrency: Int) {
        val workflowEngine = WorkflowEngine(
            clientName,
            workflowStateStorage,
            sendToClient,
            sendToTaskTag,
            sendToTaskEngine,
            sendToWorkflowTaskEngine(workflowName),
            sendToWorkflowTag,
            sendToWorkflowEngine,
            sendToWorkflowEngineAfter
        )

        with(listener) {
            val topicType = WorkflowTopics.ENGINE
            start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                { message: WorkflowEngineMessage -> workflowEngine.handle(message) },
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int) {
        with(listener) {
            val topicType = WorkflowTopics.DELAY
            start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                sendToWorkflowEngine,
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskTag(taskName: TaskName, taskTagStorage: TaskTagStorage, concurrency: Int) {
        val tagEngine = TaskTagEngine(
            clientName,
            taskTagStorage,
            sendToTaskEngine,
            sendToClient
        )

        with(listener) {
            val topicType = TaskTopics.TAG
            start<TaskTagMessage, TaskTagEnvelope>(
                { message: TaskTagMessage -> tagEngine.handle(message) },
                topicNames.topic(topicType, taskName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskEngine(taskName: TaskName, taskStateStorage: TaskStateStorage, concurrency: Int) {
        val taskEngine = TaskEngine(
            clientName,
            taskStateStorage,
            sendToClient,
            sendToTaskTag,
            sendToTaskEngineAfter,
            sendToWorkflowEngine,
            sendToTaskExecutor,
            sendToTaskMetrics,
        )

        with(listener) {
            val topicType = TaskTopics.ENGINE
            start<TaskEngineMessage, TaskEngineEnvelope>(
                { message: TaskEngineMessage -> taskEngine.handle(message) },
                topicNames.topic(topicType, taskName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskDelay(taskName: TaskName, concurrency: Int) {
        with(listener) {
            val topicType = TaskTopics.DELAY
            start<TaskEngineMessage, TaskEngineEnvelope>(
                sendToTaskEngine,
                topicNames.topic(topicType, taskName),
                SubscriptionType.Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskMetrics(taskName: TaskName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        with(listener) {
            val topicType = TaskTopics.METRICS
            start<TaskMetricsMessage, TaskMetricsEnvelope>(
                { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
                topicNames.topic(topicType, taskName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskExecutor(taskName: TaskName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToTaskEngine,
            clientFactory
        )

        with(listener) {
            val topicType = TaskTopics.EXECUTOR
            start<TaskExecutorMessage, TaskExecutorEnvelope>(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                topicNames.topic(topicType, taskName),
                SubscriptionType.Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskEngine(workflowName: WorkflowName, taskStateStorage: TaskStateStorage, concurrency: Int) {
        val taskEngine = TaskEngine(
            clientName,
            taskStateStorage,
            sendToClient,
            sendToTaskTag,
            sendToWorkflowTaskEngineAfter(workflowName),
            sendToWorkflowEngine,
            sendToWorkflowTaskExecutor(workflowName),
            sendToWorkflowTaskMetrics(workflowName),
        )

        with(listener) {
            val topicType = WorkflowTaskTopics.ENGINE
            start<TaskEngineMessage, TaskEngineEnvelope>(
                { message: TaskEngineMessage -> taskEngine.handle(message) },
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskDelay(workflowName: WorkflowName, concurrency: Int) {
        val topicType = WorkflowTaskTopics.DELAY
        with(listener) {
            start<TaskEngineMessage, TaskEngineEnvelope>(
                sendToWorkflowTaskEngine(workflowName),
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskMetrics(workflowName: WorkflowName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        with(listener) {
            val topicType = WorkflowTaskTopics.METRICS
            start<TaskMetricsMessage, TaskMetricsEnvelope>(
                { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Key_Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskExecutor(workflowName: WorkflowName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToWorkflowTaskEngine(workflowName),
            clientFactory
        )

        with(listener) {
            val topicType = WorkflowTaskTopics.EXECUTOR
            start<TaskExecutorMessage, TaskExecutorEnvelope>(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                topicNames.topic(topicType, workflowName),
                SubscriptionType.Shared,
                topicType.consumerName(clientName),
                concurrency
            )
        }
    }

    private val sendToClient: SendToClient = run {
        val topicType = ClientTopics.RESPONSE
        val producerName = topicType.consumerName(clientName)

        return@run { message: ClientMessage ->
            val topic = topicNames.topic(topicType, clientName)

            sender.send<ClientMessage, ClientEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    override val sendToTaskTag: SendToTaskTag = run {
        val topicType = TaskTopics.TAG
        val producerName = topicType.consumerName(clientName)

        return@run { message: TaskTagMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            sender.send<TaskTagMessage, TaskTagEnvelope>(
                message, zero, topic, producerName, "${message.taskTag}",
            )
        }
    }

    override val sendToTaskEngine: SendToTaskEngine = run {
        val topicType = TaskTopics.ENGINE
        val producerName = topicType.consumerName(clientName)

        return@run { message: TaskEngineMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    private val sendToTaskEngineAfter: SendToTaskEngineAfter = run {
        val topicType = TaskTopics.DELAY
        val producerName = topicType.consumerName(clientName)

        return@run { message: TaskEngineMessage, after: MillisDuration ->
            if (after > 0) {
                val topic = topicNames.topic(topicType, message.taskName)
                sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToTaskEngine(message)
            }
        }
    }

    private val sendToTaskMetrics: SendToTaskMetrics = run {
        val topicType = TaskTopics.METRICS
        val producerName = topicType.consumerName(clientName)

        return@run { message: TaskMetricsMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            sender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "${message.taskName}"
            )
        }
    }

    private val sendToTaskExecutor: SendToTaskExecutor = run {
        val topicType = TaskTopics.EXECUTOR
        val producerName = topicType.consumerName(clientName)

        return@run { message: TaskExecutorMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            sender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private fun sendToWorkflowTaskEngine(workflowName: WorkflowName): SendToTaskEngine {
        val topicType = WorkflowTaskTopics.ENGINE
        val producerName = topicType.consumerName(clientName)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskEngineMessage ->
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    private fun sendToWorkflowTaskEngineAfter(workflowName: WorkflowName): SendToTaskEngineAfter {
        val topicType = WorkflowTaskTopics.DELAY
        val producerName = topicType.consumerName(clientName)
        val topic = topicNames.topic(topicType, workflowName)
        val sendToWorkflowTaskEngine = sendToWorkflowTaskEngine(workflowName)

        return { message: TaskEngineMessage, after: MillisDuration ->
            if (after > 0) {
                sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToWorkflowTaskEngine(message)
            }
        }
    }

    private fun sendToWorkflowTaskMetrics(workflowName: WorkflowName): SendToTaskMetrics {
        val topicType = WorkflowTaskTopics.METRICS
        val producerName = topicType.consumerName(clientName)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskMetricsMessage ->
            sender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "$workflowName"
            )
        }
    }

    private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
        val topicType = WorkflowTaskTopics.EXECUTOR
        val producerName = topicType.consumerName(clientName)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskExecutorMessage ->
            sender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    override val sendToWorkflowTag: SendToWorkflowTag = run {
        val topicType = WorkflowTopics.TAG
        val producerName = topicType.consumerName(clientName)

        return@run { message: WorkflowTagMessage ->
            val topic = topicNames.topic(topicType, message.workflowName)
            sender.send<WorkflowTagMessage, WorkflowTagEnvelope>(
                message, zero, topic, producerName, "${message.workflowTag}"
            )
        }
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine = run {
        val topicType = WorkflowTopics.ENGINE
        val producerName = topicType.consumerName(clientName)

        return@run { message: WorkflowEngineMessage ->
            val topic = topicNames.topic(topicType, message.workflowName)
            sender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                message, zero, topic, producerName, "${message.workflowId}"
            )
        }
    }

    private val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = run {
        val topicType = WorkflowTopics.DELAY
        val producerName = topicType.consumerName(clientName)

        return@run { message: WorkflowEngineMessage, after: MillisDuration ->
            if (after > 0) {
                val topic = topicNames.topic(topicType, message.workflowName)
                sender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToWorkflowEngine(message)
            }
        }
    }
}
