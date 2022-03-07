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
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
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
import io.infinitic.transport.pulsar.topics.TopicType
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import kotlinx.coroutines.CoroutineScope
import org.apache.pulsar.client.api.PulsarClient

class PulsarWorkerStarter(
    client: PulsarClient,
    private val topicNames: TopicNames,
    private val workerName: String
) : WorkerStarter {

    private val zero = MillisDuration.ZERO
    private val clientName = ClientName(workerName)
    private val pulsarSender = PulsarProducer(client)
    internal val pulsarListener = PulsarConsumer(client)

    override fun CoroutineScope.startClientResponse(client: InfiniticClient) {
        start<ClientMessage, ClientEnvelope>(
            executor = { message: ClientMessage -> client.handle(message) },
            topicType = ClientTopics.RESPONSE,
            concurrency = 1,
            name = client.name
        )
    }

    override fun CoroutineScope.startWorkflowTag(workflowName: WorkflowName, workflowTagStorage: WorkflowTagStorage, concurrency: Int) {
        val tagEngine = WorkflowTagEngine(
            clientName,
            workflowTagStorage,
            sendToWorkflowEngine,
            sendToClient
        )

        start<WorkflowTagMessage, WorkflowTagEnvelope>(
            executor = { message: WorkflowTagMessage -> tagEngine.handle(message) },
            topicType = WorkflowTopics.TAG,
            concurrency = concurrency,
            name = "$workflowName"
        )
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

        start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
            executor = { message: WorkflowEngineMessage -> workflowEngine.handle(message) },
            topicType = WorkflowTopics.ENGINE,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int) {
        start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
            executor = sendToWorkflowEngine,
            topicType = WorkflowTopics.DELAY,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override fun CoroutineScope.startTaskTag(taskName: TaskName, taskTagStorage: TaskTagStorage, concurrency: Int) {
        val tagEngine = TaskTagEngine(
            clientName,
            taskTagStorage,
            sendToTaskEngine,
            sendToClient
        )

        start<TaskTagMessage, TaskTagEnvelope>(
            executor = { message: TaskTagMessage -> tagEngine.handle(message) },
            topicType = TaskTopics.TAG,
            concurrency = concurrency,
            name = "$taskName"
        )
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

        start<TaskEngineMessage, TaskEngineEnvelope>(
            executor = { message: TaskEngineMessage -> taskEngine.handle(message) },
            topicType = TaskTopics.ENGINE,
            concurrency = concurrency,
            name = "$taskName"
        )
    }

    override fun CoroutineScope.startTaskDelay(taskName: TaskName, concurrency: Int) {
        start<TaskEngineMessage, TaskEngineEnvelope>(
            executor = sendToTaskEngine,
            topicType = TaskTopics.DELAY,
            concurrency = concurrency,
            name = "$taskName"
        )
    }

    override fun CoroutineScope.startTaskMetrics(taskName: TaskName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        start<TaskMetricsMessage, TaskMetricsEnvelope>(
            executor = { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
            topicType = TaskTopics.METRICS,
            concurrency = concurrency,
            name = "$taskName"
        )
    }

    override fun CoroutineScope.startTaskExecutor(taskName: TaskName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToTaskEngine,
            clientFactory
        )

        start<TaskExecutorMessage, TaskExecutorEnvelope>(
            executor = { message: TaskExecutorMessage -> taskExecutor.handle(message) },
            topicType = TaskTopics.EXECUTOR,
            concurrency = concurrency,
            name = "$taskName"
        )
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

        start<TaskEngineMessage, TaskEngineEnvelope>(
            executor = { message: TaskEngineMessage -> taskEngine.handle(message) },
            topicType = WorkflowTaskTopics.ENGINE,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override fun CoroutineScope.startWorkflowTaskDelay(workflowName: WorkflowName, concurrency: Int) {
        start<TaskEngineMessage, TaskEngineEnvelope>(
            executor = sendToWorkflowTaskEngine(workflowName),
            topicType = WorkflowTaskTopics.DELAY,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override fun CoroutineScope.startWorkflowTaskMetrics(workflowName: WorkflowName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(taskMetricsStateStorage)

        start<TaskMetricsMessage, TaskMetricsEnvelope>(
            executor = { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
            topicType = WorkflowTaskTopics.METRICS,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override fun CoroutineScope.startWorkflowTaskExecutor(workflowName: WorkflowName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToWorkflowTaskEngine(workflowName),
            clientFactory
        )

        start<TaskExecutorMessage, TaskExecutorEnvelope>(
            executor = { message: TaskExecutorMessage -> taskExecutor.handle(message) },
            topicType = WorkflowTaskTopics.EXECUTOR,
            concurrency = concurrency,
            name = "$workflowName"
        )
    }

    override val sendToTaskTag: SendToTaskTag = run {
        val topicType = TaskTopics.TAG
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: TaskTagMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            pulsarSender.send<TaskTagMessage, TaskTagEnvelope>(
                message, zero, topic, producerName, "${message.taskTag}",
            )
        }
    }

    override val sendToTaskEngine: SendToTaskEngine = run {
        val topicType = TaskTopics.ENGINE
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: TaskEngineMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            pulsarSender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    override val sendToWorkflowTag: SendToWorkflowTag = run {
        val topicType = WorkflowTopics.TAG
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: WorkflowTagMessage ->
            val topic = topicNames.topic(topicType, message.workflowName)
            pulsarSender.send<WorkflowTagMessage, WorkflowTagEnvelope>(
                message, zero, topic, producerName, "${message.workflowTag}"
            )
        }
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine = run {
        val topicType = WorkflowTopics.ENGINE
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: WorkflowEngineMessage ->
            val topic = topicNames.topic(topicType, message.workflowName)
            pulsarSender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                message, zero, topic, producerName, "${message.workflowId}"
            )
        }
    }

    private val sendToClient: SendToClient = run {
        val topicType = ClientTopics.RESPONSE
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: ClientMessage ->
            val topic = topicNames.topic(topicType, workerName)
            pulsarSender.send<ClientMessage, ClientEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToTaskEngineAfter: SendToTaskEngineAfter = run {
        val topicType = TaskTopics.DELAY
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: TaskEngineMessage, after: MillisDuration ->
            if (after > 0) {
                val topic = topicNames.topic(topicType, message.taskName)
                pulsarSender.send<TaskEngineMessage, TaskEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToTaskEngine(message)
            }
        }
    }

    private val sendToTaskMetrics: SendToTaskMetrics = run {
        val topicType = TaskTopics.METRICS
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: TaskMetricsMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            pulsarSender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "${message.taskName}"
            )
        }
    }

    private val sendToTaskExecutor: SendToTaskExecutor = run {
        val topicType = TaskTopics.EXECUTOR
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: TaskExecutorMessage ->
            val topic = topicNames.topic(topicType, message.taskName)
            pulsarSender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private fun sendToWorkflowTaskEngine(workflowName: WorkflowName): SendToTaskEngine {
        val topicType = WorkflowTaskTopics.ENGINE
        val producerName = topicNames.producerName(workerName, topicType)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskEngineMessage ->
            pulsarSender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    private fun sendToWorkflowTaskEngineAfter(workflowName: WorkflowName): SendToTaskEngineAfter {
        val topicType = WorkflowTaskTopics.DELAY
        val producerName = topicNames.producerName(workerName, topicType)
        val topic = topicNames.topic(topicType, workflowName)
        val sendToWorkflowTaskEngine = sendToWorkflowTaskEngine(workflowName)

        return { message: TaskEngineMessage, after: MillisDuration ->
            if (after > 0) {
                pulsarSender.send<TaskEngineMessage, TaskEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToWorkflowTaskEngine(message)
            }
        }
    }

    private fun sendToWorkflowTaskMetrics(workflowName: WorkflowName): SendToTaskMetrics {
        val topicType = WorkflowTaskTopics.METRICS
        val producerName = topicNames.producerName(workerName, topicType)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskMetricsMessage ->
            pulsarSender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "$workflowName"
            )
        }
    }

    private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
        val topicType = WorkflowTaskTopics.EXECUTOR
        val producerName = topicNames.producerName(workerName, topicType)
        val topic = topicNames.topic(topicType, workflowName)

        return { message: TaskExecutorMessage ->
            pulsarSender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = run {
        val topicType = WorkflowTopics.DELAY
        val producerName = topicNames.producerName(workerName, topicType)

        return@run { message: WorkflowEngineMessage, after: MillisDuration ->
            if (after > 0) {
                val topic = topicNames.topic(topicType, message.workflowName)
                pulsarSender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                    message, after, topic, producerName
                )
            } else {
                sendToWorkflowEngine(message)
            }
        }
    }

    internal inline fun <T : Message, reified S : Envelope<T>> CoroutineScope.start(
        crossinline executor: suspend (T) -> Unit,
        topicType: TopicType,
        concurrency: Int,
        name: String
    ) {
        with(pulsarListener) {
            val topic = topicNames.topic(topicType, name)
            startConsumer<T, S>(
                executor = executor,
                topic = topic,
                subscriptionName = topicType.subscriptionName,
                subscriptionType = topicType.subscriptionType,
                consumerName = topicNames.consumerName(workerName, topicType),
                concurrency = concurrency,
                topicDLQ = topicNames.deadLetterQueue(topic)
            )
        }
    }
}
