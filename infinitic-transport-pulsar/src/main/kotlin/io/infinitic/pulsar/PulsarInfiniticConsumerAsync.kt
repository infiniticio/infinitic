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
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.events.TaskEventEnvelope
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicsDescription
import io.infinitic.pulsar.resources.TopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicsDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

class PulsarInfiniticConsumerAsync(
  private val consumer: Consumer,
  private val resourceManager: ResourceManager,
  val shutdownGracePeriodInSeconds: Double
) : InfiniticConsumerAsync {

  override var logName: String? = null

  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  /** Coroutine scope used to receive messages */
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  private lateinit var clientName: String

  override fun close() {
    // we test if consumingScope is active, just in case
    // the user tries to manually close an already closed resource
    if (consumingScope.isActive) {
      // delete consumingScope, all ongoing messages should be processed properly
      consumingScope.cancel()
      runBlocking {
        try {
          withTimeout((shutdownGracePeriodInSeconds * 1000L).toLong()) {
            coroutineScope {
              launch { deleteClientTopic() }
              launch {
                logger.info { "Processing ongoing messages..." }
                consumingScope.coroutineContext.job.children.forEach { it.join() }
                logger.info { "All ongoing messages have been processed." }
              }
            }
            // once the messages are processed, we can close other resources (pulsar client & admin)
            autoClose()
          }
        } catch (e: TimeoutCancellationException) {
          logger.warn {
            "The grace period (${shutdownGracePeriodInSeconds}s) allotted to close was insufficient. " +
                "Some ongoing messages may not have been processed properly."
          }
        }
      }
    }
  }

  private fun deleteClientTopic() {
    if (::clientName.isInitialized) {
      val clientTopic = resourceManager.getTopicName(clientName, ClientTopicDescription.RESPONSE)
      logger.info { "Deleting client topic: '$clientTopic'" }
      resourceManager.deleteTopic(clientTopic)
          .onFailure { logger.warn(it) { "Unable to delete client topic: '$clientTopic'." } }
          .onSuccess { logger.info { "Client topic deleted: '$clientTopic'." } }
    }
  }

  // Start consumers of messages to client
  override fun startClientConsumerAsync(
    handler: (ClientMessage, MillisInstant) -> Unit,
    beforeDlq: ((ClientMessage, Exception) -> Unit)?,
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
    handler: (WorkflowTagMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowTagMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowTagEnvelope::class,
      topicDescription = WorkflowTopicsDescription.TAG,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowCmdConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEngineEnvelope::class,
      topicDescription = WorkflowTopicsDescription.CMD,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to workflow engine
  override fun startWorkflowEngineConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEngineEnvelope::class,
      topicDescription = WorkflowTopicsDescription.ENGINE,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of delayed messages to workflow engine
  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEngineEnvelope::class,
      topicDescription = WorkflowTopicsDescription.ENGINE_DELAYED,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowEventsConsumerAsync(
    handler: (WorkflowEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEventMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = WorkflowEventEnvelope::class,
      topicDescription = WorkflowTopicsDescription.EVENTS,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to task tags
  override fun startTaskTagConsumerAsync(
    handler: (TaskTagMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskTagMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskTagEnvelope::class,
      topicDescription = ServiceTopicsDescription.TAG,
      concurrency = concurrency,
      name = "$serviceName",
  )

  // Start consumers of messages to task executor
  override fun startTaskExecutorConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskExecutorEnvelope::class,
      topicDescription = ServiceTopicsDescription.EXECUTOR,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startTaskEventsConsumerAsync(
    handler: (TaskEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskEventMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskEventEnvelope::class,
      topicDescription = ServiceTopicsDescription.EVENTS,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start consumers of messages to workflow task executor
  override fun startWorkflowTaskConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskExecutorEnvelope::class,
      topicDescription = WorkflowTopicsDescription.TASK_EXECUTOR,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowTaskEventsConsumerAsync(
    handler: (TaskEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskEventMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      schemaClass = TaskEventEnvelope::class,
      topicDescription = WorkflowTopicsDescription.TASK_EVENTS,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start a consumer on a topic, with concurrent executors
  private fun <T : Message, S : Envelope<out T>> startAsync(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    schemaClass: KClass<S>,
    topicDescription: TopicDescription,
    concurrency: Int,
    name: String
  ): CompletableFuture<Unit> {
    return with(consumer) {
      consumingScope.future {
        
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

        runConsumer(
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
