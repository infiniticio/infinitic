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
package io.infinitic.pulsar

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.autoclose.autoClose
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.ClientType
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceType
import io.infinitic.pulsar.resources.TopicType
import io.infinitic.pulsar.resources.WorkflowTaskType
import io.infinitic.pulsar.resources.WorkflowType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class PulsarInfiniticConsumer(
  private val consumer: Consumer,
  private val resourceManager: ResourceManager
) : InfiniticConsumer {

  private val logger = KotlinLogging.logger {}

  /** Coroutine scope used to receive messages */
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  private lateinit var clientName: String

  override fun close() {
    consumingScope.cancel()
    // Delete client topic
    if (::clientName.isInitialized) {
      val topic = resourceManager.getTopicName(clientName, ClientType.RESPONSE)
      logger.info { "Deleting response topic $topic of client $clientName" }
      resourceManager.deleteTopic(topic)
    }
    autoClose()
  }

  // Start consumers of messages to client
  override fun startClientConsumerAsync(
    handler: suspend (ClientMessage) -> Unit,
    clientName: ClientName
  ): CompletableFuture<Unit> = startAsync<ClientMessage, ClientEnvelope>(
      handler = handler,
      topicType = ClientType.RESPONSE,
      concurrency = 1,
      name = "$clientName",
  ).also { this.clientName = "$clientName" }

  // Start consumers of messages to workflow tag
  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<WorkflowTagMessage, WorkflowTagEnvelope>(
      handler = handler,
      topicType = WorkflowType.TAG,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to workflow engine
  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<WorkflowEngineMessage, WorkflowEngineEnvelope>(
      handler = handler,
      topicType = WorkflowType.ENGINE,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of delayed messages to workflow engine
  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<WorkflowEngineMessage, WorkflowEngineEnvelope>(
      handler = handler,
      topicType = WorkflowType.DELAY,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to task tags
  override fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync<TaskTagMessage, TaskTagEnvelope>(
      handler = handler,
      topicType = ServiceType.TAG,
      concurrency = concurrency,
      name = "$serviceName",
  )

  // Start consumers of messages to task executor
  override fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync<TaskExecutorMessage, TaskExecutorEnvelope>(
      handler = handler,
      topicType = ServiceType.EXECUTOR,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start consumers of messages to workflow task executor
  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<TaskExecutorMessage, TaskExecutorEnvelope>(
      handler = handler,
      topicType = WorkflowTaskType.EXECUTOR,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start a consumer on a topic, with concurrent executors
  private inline fun <T : Message, reified S : Envelope<T>> startAsync(
    noinline handler: suspend (T) -> Unit, topicType: TopicType, concurrency: Int, name: String
  ): CompletableFuture<Unit> {
    // create topic if not exists
    val topic = resourceManager.initTopic(name, topicType).getOrThrow()
    // create DLQ topic if not exists
    val dlq = resourceManager.initDLQTopic(name, topicType).getOrThrow()

    return with(consumer) {
      consumingScope.future {
        startConsumer<T, S>(
            handler = handler,
            topic = topic,
            subscriptionName = topicType.subscriptionName,
            subscriptionType = topicType.subscriptionType,
            consumerName = resourceManager.getConsumerName(name, topicType),
            concurrency = concurrency,
            topicDLQ = dlq,
        )
      }
    }
  }
}
