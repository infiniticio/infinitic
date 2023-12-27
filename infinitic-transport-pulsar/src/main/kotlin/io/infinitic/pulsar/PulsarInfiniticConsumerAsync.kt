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
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicDescription
import io.infinitic.pulsar.resources.TopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

class PulsarInfiniticConsumerAsync(
  private val consumer: Consumer,
  private val resourceManager: ResourceManager
) : InfiniticConsumerAsync {

  private val logger = KotlinLogging.logger {}

  /** Coroutine scope used to receive messages */
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  private lateinit var clientName: String

  override fun close() {
    consumingScope.cancel()
    // Delete client topic
    if (::clientName.isInitialized) {
      val topic = resourceManager.getTopicName(clientName, ClientTopicDescription.RESPONSE)
      logger.info { "Deleting response topic $topic of client $clientName" }
      resourceManager.deleteTopic(topic)
    }
    autoClose()
  }

  // Start consumers of messages to client
  override fun startClientConsumerAsync(
    handler: suspend (ClientMessage) -> Unit,
    beforeDlq: (suspend (ClientMessage, Exception) -> Unit)?,
    clientName: ClientName
  ): CompletableFuture<Unit> = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = ClientEnvelope::class,
      topicDescription = ClientTopicDescription.RESPONSE,
      concurrency = 1,
      name = "$clientName",
  ).also { this.clientName = "$clientName" }

  // Start consumers of messages to workflow tag
  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    beforeDlq: (suspend (WorkflowTagMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowTagEnvelope::class,
      topicDescription = WorkflowTopicDescription.TAG,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to workflow engine
  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEngineEnvelope::class,
      topicDescription = WorkflowTopicDescription.ENGINE,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of delayed messages to workflow engine
  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEngineEnvelope::class,
      topicDescription = WorkflowTopicDescription.ENGINE_DELAYED,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to task tags
  override fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    beforeDlq: (suspend (TaskTagMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskTagEnvelope::class,
      topicDescription = ServiceTopicDescription.TAG,
      concurrency = concurrency,
      name = "$serviceName",
  )

  // Start consumers of messages to task executor
  override fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskExecutorEnvelope::class,
      topicDescription = ServiceTopicDescription.EXECUTOR,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start consumers of messages to workflow task executor
  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskExecutorEnvelope::class,
      topicDescription = WorkflowTopicDescription.EXECUTOR,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start a consumer on a topic, with concurrent executors
  private fun <T : Message, S : Envelope<out T>> startAsync(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    schemaClass: KClass<S>,
    topicDescription: TopicDescription,
    concurrency: Int,
    name: String
  ): CompletableFuture<Unit> {
    val topic = resourceManager.getTopicName(name, topicDescription)
    // create topic if needed
    resourceManager.initTopicOnce(
        topic = topic,
        isPartitioned = topicDescription.isPartitioned,
        isDelayed = topicDescription.isDelayed,
    )

    val topicDlq = resourceManager.getDlqTopicName(name, topicDescription)
    // create DLQ topic if needed
    resourceManager.initDlqTopicOnce(
        topic = topicDlq,
        isPartitioned = topicDescription.isPartitioned,
        isDelayed = topicDescription.isDelayed,
    )

    return with(consumer) {
      consumingScope.future {
        startConsumer(
            handler = handler,
            beforeDlq = beforeDlq,
            schemaClass = schemaClass,
            topic = topic,
            topicDlq = topicDlq,
            subscriptionName = topicDescription.subscriptionName,
            subscriptionNameDlq = topicDescription.subscriptionNameDlq,
            subscriptionType = topicDescription.subscriptionType,
            consumerName = resourceManager.getConsumerName(name, topicDescription),
            concurrency = concurrency,
        )
      }
    }
  }
}
