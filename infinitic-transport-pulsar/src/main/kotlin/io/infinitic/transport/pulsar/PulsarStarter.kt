/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.transport.pulsar

import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.ClientStarter
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.common.workers.registry.WorkerRegistry
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
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.transport.pulsar.config.topics.ConsumerConfig
import io.infinitic.transport.pulsar.config.topics.ProducerConfig
import io.infinitic.transport.pulsar.topics.ClientTopics
import io.infinitic.transport.pulsar.topics.ServiceTopics
import io.infinitic.transport.pulsar.topics.TopicNames
import io.infinitic.transport.pulsar.topics.TopicType
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient

class PulsarStarter(
    client: PulsarClient,
    private val topicNames: TopicNames,
    private val workerName: String,
    producerConfig: ProducerConfig,
    consumerConfig: ConsumerConfig
) : ClientStarter, WorkerStarter {
  private val logger = KotlinLogging.logger {}

  private val zero = MillisDuration.ZERO
  private val clientName = ClientName(workerName)
  private val pulsarProducer = PulsarProducer(client, producerConfig)
  private val pulsarConsumer = PulsarConsumer(client, consumerConfig)

  override fun CoroutineScope.startWorkflowTag(
      workflowName: WorkflowName,
      workflowTagStorage: WorkflowTagStorage,
      concurrency: Int
  ) {
    val tagEngine =
        WorkflowTagEngine(
            clientName, workflowTagStorage, sendToWorkflowTag, sendToWorkflowEngine, sendToClient)

    start<WorkflowTagMessage, WorkflowTagEnvelope>(
        executor = { message: WorkflowTagMessage -> tagEngine.handle(message) },
        topicType = WorkflowTopics.TAG,
        concurrency = concurrency,
        name = "$workflowName")
  }

  override fun CoroutineScope.startWorkflowEngine(
      workflowName: WorkflowName,
      workflowStateStorage: WorkflowStateStorage,
      concurrency: Int
  ) {
    val workflowEngine =
        WorkflowEngine(
            clientName,
            workflowStateStorage,
            sendToClient,
            sendToTaskTag,
            sendToTaskExecutor,
            sendToWorkflowTaskExecutor(workflowName),
            sendToWorkflowTag,
            sendToWorkflowEngine,
            sendToWorkflowEngineAfter)

    start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
        executor = { message: WorkflowEngineMessage -> workflowEngine.handle(message) },
        topicType = WorkflowTopics.ENGINE,
        concurrency = concurrency,
        name = "$workflowName")
  }

  override fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int) {
    start<WorkflowEngineMessage, WorkflowEngineEnvelope>(
        executor = sendToWorkflowEngine,
        topicType = WorkflowTopics.DELAY,
        concurrency = concurrency,
        name = "$workflowName")
  }

  override fun CoroutineScope.startTaskTag(
      serviceName: ServiceName,
      taskTagStorage: TaskTagStorage,
      concurrency: Int
  ) {
    val tagEngine = TaskTagEngine(clientName, taskTagStorage, sendToClient)

    start<TaskTagMessage, TaskTagEnvelope>(
        executor = { message: TaskTagMessage -> tagEngine.handle(message) },
        topicType = ServiceTopics.TAG,
        concurrency = concurrency,
        name = "$serviceName")
  }

  override fun CoroutineScope.startTaskExecutor(
      serviceName: ServiceName,
      concurrency: Int,
      workerRegistry: WorkerRegistry,
      clientFactory: ClientFactory
  ) {
    val taskExecutor =
        TaskExecutor(
            clientName,
            workerRegistry,
            sendToTaskExecutorAfter,
            sendToTaskTag,
            sendToWorkflowEngine,
            sendToClient,
            clientFactory)

    start<TaskExecutorMessage, TaskExecutorEnvelope>(
        executor = { message: TaskExecutorMessage -> taskExecutor.handle(message) },
        topicType = ServiceTopics.EXECUTOR,
        concurrency = concurrency,
        name = "$serviceName")
  }

  override fun CoroutineScope.startWorkflowTaskExecutor(
      workflowName: WorkflowName,
      concurrency: Int,
      workerRegistry: WorkerRegistry,
      clientFactory: ClientFactory
  ) {
    val taskExecutor =
        TaskExecutor(
            clientName,
            workerRegistry,
            sendToWorkflowTaskExecutorAfter(workflowName),
            {}, // Workflow tasks do not have tags
            sendToWorkflowEngine,
            sendToClient,
            clientFactory)

    start<TaskExecutorMessage, TaskExecutorEnvelope>(
        executor = { message: TaskExecutorMessage -> taskExecutor.handle(message) },
        topicType = WorkflowTaskTopics.EXECUTOR,
        concurrency = concurrency,
        name = "$workflowName")
  }

  override fun CoroutineScope.startClientResponse(client: InfiniticClientInterface) {
    start<ClientMessage, ClientEnvelope>(
        executor = { message: ClientMessage -> client.handle(message) },
        topicType = ClientTopics.RESPONSE,
        concurrency = 1,
        name = client.name)
  }

  override val sendToWorkflowTag: SendToWorkflowTag = run {
    val topicType = WorkflowTopics.TAG
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: WorkflowTagMessage ->
      val topic = topicNames.topic(topicType, message.workflowName)
      pulsarProducer.send<WorkflowTagMessage, WorkflowTagEnvelope>(
          message, zero, topic, producerName, "${message.workflowTag}")
    }
  }

  private val sendToTaskTag: SendToTaskTag = run {
    val topicType = ServiceTopics.TAG
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: TaskTagMessage ->
      val topic = topicNames.topic(topicType, message.serviceName)
      pulsarProducer.send<TaskTagMessage, TaskTagEnvelope>(
          message, zero, topic, producerName, "${message.taskTag}")
    }
  }

  private val sendToTaskExecutor: SendToTaskExecutor = run {
    val topicType = ServiceTopics.EXECUTOR
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: TaskExecutorMessage ->
      val topic = topicNames.topic(topicType, message.serviceName)
      pulsarProducer.send<TaskExecutorMessage, TaskExecutorEnvelope>(
          message, zero, topic, producerName)
    }
  }

  override val sendToWorkflowEngine: SendToWorkflowEngine = run {
    val topicType = WorkflowTopics.ENGINE
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: WorkflowEngineMessage ->
      val topic = topicNames.topic(topicType, message.workflowName)
      pulsarProducer.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
          message, zero, topic, producerName, "${message.workflowId}")
    }
  }

  private val sendToClient: SendToClient = run {
    val topicType = ClientTopics.RESPONSE
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: ClientMessage ->
      val topic = topicNames.topic(topicType, message.recipientName)
      pulsarProducer.send<ClientMessage, ClientEnvelope>(message, zero, topic, producerName)
    }
  }

  private val sendToTaskExecutorAfter: SendToTaskExecutorAfter = run {
    val topicType = ServiceTopics.EXECUTOR
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: TaskExecutorMessage, after: MillisDuration ->
      val topic = topicNames.topic(topicType, message.serviceName)
      pulsarProducer.send<TaskExecutorMessage, TaskExecutorEnvelope>(
          message, after, topic, producerName)
    }
  }

  private fun sendToWorkflowTaskExecutor(workflowName: WorkflowName): SendToTaskExecutor {
    val topicType = WorkflowTaskTopics.EXECUTOR
    val producerName = topicNames.producerName(workerName, topicType)
    val topic = topicNames.topic(topicType, workflowName)

    return { message: TaskExecutorMessage ->
      pulsarProducer.send<TaskExecutorMessage, TaskExecutorEnvelope>(
          message, zero, topic, producerName)
    }
  }

  private fun sendToWorkflowTaskExecutorAfter(workflowName: WorkflowName): SendToTaskExecutorAfter {
    val topicType = WorkflowTaskTopics.EXECUTOR
    val producerName = topicNames.producerName(workerName, topicType)
    val topic = topicNames.topic(topicType, workflowName)

    return { message: TaskExecutorMessage, after: MillisDuration ->
      pulsarProducer.send<TaskExecutorMessage, TaskExecutorEnvelope>(
          message, after, topic, producerName)
    }
  }

  private val sendToWorkflowEngineAfter: SendToWorkflowEngineAfter = run {
    val topicType = WorkflowTopics.DELAY
    val producerName = topicNames.producerName(workerName, topicType)

    return@run { message: WorkflowEngineMessage, after: MillisDuration ->
      if (after > 0) {
        val topic = topicNames.topic(topicType, message.workflowName)
        pulsarProducer.send<WorkflowEngineMessage, WorkflowEngineEnvelope>(
            message, after, topic, producerName)
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
    if (isActive) {
      with(pulsarConsumer) {
        startConsumer<T, S>(
            executor = executor,
            topic = topicNames.topic(topicType, name),
            subscriptionName = topicType.subscriptionName,
            subscriptionType = topicType.subscriptionType,
            consumerName = topicNames.consumerName(workerName, topicType),
            concurrency = concurrency,
            topicDLQ = topicNames.topicDLQ(topicType, name))
      }
    } else {
      logger.warn(
          "Coroutine not active, cannot start consumer ${topicNames.consumerName(workerName, topicType)}")
    }
  }
}
