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
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.SendToGlobalMetrics
import io.infinitic.common.metrics.global.messages.GlobalMetricsEnvelope
import io.infinitic.common.metrics.global.messages.GlobalMetricsMessage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.storage.TaskStateStorage
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
import io.infinitic.metrics.perName.engine.TaskMetricsEngine
import io.infinitic.tags.tasks.TaskTagEngine
import io.infinitic.tags.workflows.WorkflowTagEngine
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.transport.pulsar.topics.Topics
import io.infinitic.workflows.engine.WorkflowEngine
import kotlinx.coroutines.CoroutineScope
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType

class PulsarStarter(val topics: Topics, val client: PulsarClient, val name: String) : WorkerStarter {
    private val zero = MillisDuration(0L)
    private val clientName = ClientName(name)
    private val sender = Sender(client)
    private val listener = Listener(client)

    override fun CoroutineScope.startClientResponse(clientHandler: suspend (ClientMessage) -> Unit, clientName: ClientName) {
        with(listener) {
            start<ClientMessage, ClientEnvelope>(
                clientHandler,
                topics.clients(clientName).response,
                SubscriptionType.Exclusive,
                "$clientName:${Topics.CLIENT}"
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
            start<WorkflowTagMessage, WorkflowTagEnvelope>(
                { message: WorkflowTagMessage -> tagEngine.handle(message) },
                topics.workflows(workflowName).tag,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.WORKFLOW_TAG}",
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
            start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                { message: WorkflowEngineMessage -> workflowEngine.handle(message) },
                topics.workflows(workflowName).engine,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.WORKFLOW_ENGINE}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int) {
        with(listener) {
            start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                sendToWorkflowEngine,
                topics.workflows(workflowName).delay,
                SubscriptionType.Shared,
                "$clientName:${Topics.WORKFLOW_DELAY}",
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
            start<TaskTagMessage, TaskTagEnvelope>(
                { message: TaskTagMessage -> tagEngine.handle(message) },
                topics.tasks(taskName).tag,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.TASK_TAG}",
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
            start<TaskEngineMessage, TaskEngineEnvelope>(
                { message: TaskEngineMessage -> taskEngine.handle(message) },
                topics.tasks(taskName).engine,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.TASK_ENGINE}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskMetrics(taskName: TaskName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(
            clientName,
            taskMetricsStateStorage,
            sendToGlobalMetrics
        )

        with(listener) {
            start<TaskMetricsMessage, TaskMetricsEnvelope>(
                { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
                topics.tasks(taskName).metrics,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.TASK_METRICS}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startTaskDelay(taskName: TaskName, concurrency: Int) {
        with(listener) {
            start<TaskEngineMessage, TaskEngineEnvelope>(
                sendToTaskEngine,
                topics.tasks(taskName).delay,
                SubscriptionType.Shared,
                "$clientName:${Topics.TASK_DELAY}",
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
            start<TaskExecutorMessage, TaskExecutorEnvelope>(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                topics.tasks(taskName).executor,
                SubscriptionType.Shared,
                "$clientName:${Topics.WORKFLOW_TASK_EXECUTOR}",
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
            sendToTaskExecutor,
            sendToTaskMetrics,
        )

        with(listener) {
            start<TaskEngineMessage, TaskEngineEnvelope>(
                { message: TaskEngineMessage -> taskEngine.handle(message) },
                topics.workflowTasks(workflowName).engine,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.WORKFLOW_TASK_ENGINE}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskMetrics(workflowName: WorkflowName, taskMetricsStateStorage: TaskMetricsStateStorage, concurrency: Int) {
        val taskMetricsEngine = TaskMetricsEngine(
            clientName,
            taskMetricsStateStorage,
            sendToGlobalMetrics
        )

        with(listener) {
            start<TaskMetricsMessage, TaskMetricsEnvelope>(
                { message: TaskMetricsMessage -> taskMetricsEngine.handle(message) },
                topics.workflowTasks(workflowName).metrics,
                SubscriptionType.Key_Shared,
                "$clientName:${Topics.WORKFLOW_TASK_METRICS}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskDelay(workflowName: WorkflowName, concurrency: Int) {
        with(listener) {
            start<TaskEngineMessage, TaskEngineEnvelope>(
                sendToTaskEngine,
                topics.workflowTasks(workflowName).delay,
                SubscriptionType.Shared,
                "$clientName:${Topics.WORKFLOW_TASK_DELAY}",
                concurrency
            )
        }
    }

    override fun CoroutineScope.startWorkflowTaskExecutor(workflowName: WorkflowName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory) {
        val taskExecutor = TaskExecutor(
            clientName,
            workerRegister,
            sendToTaskEngine,
            clientFactory
        )

        with(listener) {
            start<TaskExecutorMessage, TaskExecutorEnvelope>(
                { message: TaskExecutorMessage -> taskExecutor.handle(message) },
                topics.workflowTasks(workflowName).executor,
                SubscriptionType.Shared,
                "$clientName:${Topics.WORKFLOW_TASK_EXECUTOR}",
                concurrency
            )
        }
    }

    private val sendToClient: SendToClient = run {
        val producerName = "$clientName:${Topics.CLIENT}"

        return@run { message: ClientMessage ->
            val topic = topics.clients(message.recipientName).response

            sender.send<ClientMessage, ClientEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToGlobalMetrics: SendToGlobalMetrics = run {
        val producerName = "$clientName:${Topics.GLOBAL_METRICS}"
        val topic = topics.global().metrics

        return@run { message: GlobalMetricsMessage ->
            sender.send<GlobalMetricsMessage, GlobalMetricsEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToTaskTag: SendToTaskTag = run {
        val producerName = "$clientName:${Topics.TASK_TAG}"

        return@run { message: TaskTagMessage ->
            val topic = topics.tasks(message.taskName).tag
            sender.send<TaskTagMessage, TaskTagEnvelope>(
                message, zero, topic, producerName, "${message.taskTag}",
            )
        }
    }

    private val sendToTaskEngine: SendToTaskEngine = run {
        val producerName = "$clientName:${Topics.TASK_ENGINE}"

        return@run { message: TaskEngineMessage ->
            val topic = topics.tasks(message.taskName).engine
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    private val sendToTaskEngineAfter: SendToTaskEngineAfter = run {
        val producerName = "$clientName:${Topics.TASK_ENGINE}"

        return@run { message: TaskEngineMessage, after: MillisDuration ->
            val topic = if (after > 0) {
                topics.tasks(message.taskName).delay
            } else {
                topics.tasks(message.taskName).engine
            }
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, after, topic, producerName, "${message.taskId}"
            )
        }
    }

    private val sendToTaskDelay: SendToTaskEngine = run {
        val producerName = "$clientName:${Topics.TASK_DELAY}"

        return@run { message: TaskEngineMessage ->
            val topic = topics.tasks(message.taskName).delay
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToTaskMetrics: SendToTaskMetrics = run {
        val producerName = "$clientName:${Topics.TASK_METRICS}"

        return@run { message: TaskMetricsMessage ->
            val topic = topics.tasks(message.taskName).metrics
            sender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "${message.taskName}"
            )
        }
    }

    private val sendToTaskExecutor: SendToTaskExecutor = run {
        val producerName = "$clientName:${Topics.TASK_EXECUTOR}"

        return@run { message: TaskExecutorMessage ->
            val topic = topics.tasks(message.taskName).executor
            sender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private fun sendToWorkflowTaskEngine(workflowName: WorkflowName): SendToTaskEngine {
        val topic = topics.workflowTasks(workflowName).engine
        val producerName = "$clientName:${Topics.WORKFLOW_TASK_ENGINE}"

        return { message: TaskEngineMessage ->
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName, "${message.taskId}"
            )
        }
    }

    private fun sendToWorkflowTaskEngineAfter(workflowName: WorkflowName): SendToTaskEngineAfter = run {
        val producerName = "$clientName:${Topics.TASK_ENGINE}"

        return@run { message: TaskEngineMessage, after: MillisDuration ->
            val topic = if (after > 0) {
                topics.workflowTasks(workflowName).delay
            } else {
                topics.workflowTasks(workflowName).engine
            }
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, after, topic, producerName, "${message.taskId}"
            )
        }
    }

    private fun sendToWorkflowTaskDelay(workflowName: WorkflowName): SendToTaskEngine {
        val topic = topics.workflowTasks(workflowName).delay
        val producerName = "$clientName:${Topics.WORKFLOW_TASK_DELAY}"

        return { message: TaskEngineMessage ->
            sender.send<TaskEngineMessage, TaskEngineEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private fun sendToWorkflowTaskMetrics(workflowName: WorkflowName): SendToTaskMetrics {
        val topic = topics.workflowTasks(workflowName).metrics
        val producerName = "$clientName:${Topics.WORKFLOW_TASK_METRICS}"

        return { message: TaskMetricsMessage ->
            sender.send<TaskMetricsMessage, TaskMetricsEnvelope>(
                message, zero, topic, producerName, "$workflowName"
            )
        }
    }

    private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
        val topic = topics.workflowTasks(workflowName).executor
        val producerName = "$clientName:${Topics.WORKFLOW_TASK_EXECUTOR}"

        return { message: TaskExecutorMessage ->
            sender.send<TaskExecutorMessage, TaskExecutorEnvelope>(
                message, zero, topic, producerName
            )
        }
    }

    private val sendToWorkflowTag: SendToWorkflowTag = run {
        val producerName = "$clientName:${Topics.WORKFLOW_TAG}"

        return@run { message: WorkflowTagMessage ->
            val topic = topics.workflows(message.workflowName).tag
            sender.send<WorkflowTagMessage, WorkflowTagEnvelope>(
                message, zero, topic, producerName, "${message.workflowTag}"
            )
        }
    }

    private val sendToWorkflowEngine: SendToWorkflowEngine = run {
        val producerName = "$clientName:${Topics.WORKFLOW_ENGINE}"

        return@run { message: WorkflowEngineMessage ->
            val topic = topics.workflows(message.workflowName).engine
            sender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                message, zero, topic, producerName, "${message.workflowId}"
            )
        }
    }

    private val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = run {
        val producerName = "$clientName:${Topics.WORKFLOW_ENGINE}"

        return@run { message: WorkflowEngineMessage, after: MillisDuration ->
            val topic = if (after > 0) {
                topics.workflows(message.workflowName).delay
            } else {
                topics.workflows(message.workflowName).engine
            }
            sender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                message, after, topic, producerName, "${message.workflowId}"
            )
        }
    }

    private fun sendToWorkflowDelay(workflowName: WorkflowName): SendToWorkflowEngine {
        val topic = topics.workflows(workflowName).delay
        val producerName = "$clientName:${Topics.WORKFLOW_DELAY}"

        return { message: WorkflowEngineMessage ->
            sender.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
                message, zero, topic, producerName
            )
        }
    }
}
