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
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.topics.ServiceEventsTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowCmdTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.topics.WorkflowServiceEventsTopic
import io.infinitic.common.topics.WorkflowServiceExecutorTopic
import io.infinitic.common.topics.WorkflowTagTopic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.MainSubscription
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.schema
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture

class PulsarInfiniticConsumerAsync(
  private val consumer: Consumer,
  private val pulsarResources: PulsarResources,
  val shutdownGracePeriodInSeconds: Double
) : InfiniticConsumerAsync {

  override var logName: String? = null

  override fun <S : Message> startConsumerAsync(
    topic: Topic<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    entity: String
  ): CompletableFuture<Unit> {

    TODO("Not yet implemented")
  }

  // See InfiniticWorker
  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  private lateinit var clientName: String

  override fun close() {
    // we test if consumingScope is active, just in case
    // the user tries to manually close an already closed resource
    if (consumer.isActive) {
      runBlocking {
        try {
          withTimeout((shutdownGracePeriodInSeconds * 1000L).toLong()) {
            consumer.cancel()
            logger.info { "Processing ongoing messages..." }
            consumer.join()
            logger.info { "All ongoing messages have been processed." }
            // delete client topic only after
            deleteClientTopic()
          }
          // once the messages are processed, we can close other resources (pulsar client & admin)
          autoClose()
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
      val clientTopic = with(pulsarResources) { ClientTopic.fullName(clientName) }
      logger.debug { "Deleting client topic '$clientTopic'." }
      pulsarResources.deleteTopic(clientTopic)
          .onFailure { logger.warn(it) { "Unable to delete client topic '$clientTopic'." } }
          .onSuccess { logger.info { "Client topic '$clientTopic' deleted." } }
    }
  }

  // Start consumers of messages to client
  override fun startClientConsumerAsync(
    handler: suspend (ClientMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ClientMessage?, Exception) -> Unit,
    clientName: ClientName
  ): CompletableFuture<Unit> = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = ClientTopic,
      concurrency = 1,
      name = "$clientName",
  ).also { this.clientName = "$clientName" }

  // Start consumers of messages to workflow tag
  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowTagMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowTagTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowCmdConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowCmdTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to workflow engine
  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowEngineTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of delayed messages to workflow engine
  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = DelayedWorkflowEngineTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowEventsConsumerAsync(
    handler: suspend (WorkflowEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEventMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowEventsTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of messages to task tags
  override fun startTaskTagConsumerAsync(
    handler: suspend (ServiceTagMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceTagMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = ServiceTagTopic,
      concurrency = concurrency,
      name = "$serviceName",
  )

  // Start consumers of messages to task executor
  override fun startTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = ServiceExecutorTopic,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceEventMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = ServiceEventsTopic,
      concurrency = concurrency,
      name = "$serviceName",
  )

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start consumers of messages to workflow task executor
  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowServiceExecutorTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startWorkflowTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceEventMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync(
      handler = handler,
      beforeDlq = beforeDlq,
      topic = WorkflowServiceEventsTopic,
      concurrency = concurrency,
      name = "$workflowName",
  )

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    // Nothing to do
    return CompletableFuture.completedFuture(Unit)
  }

  // Start a consumer on a topic, with concurrent executors
  private fun <T : Message> startAsync(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    topic: Topic<T>,
    concurrency: Int,
    name: String
  ): CompletableFuture<Unit> {

    // name of topic
    val topicName = with(pulsarResources) { topic.fullName(name) }

    // name of DLQ topic
    val topicDLQName = with(pulsarResources) { topic.fullNameDLQ(name) }

    return consumer.runAsync(
        handler = handler,
        beforeDlq = beforeDlq,
        schema = topic.schema,
        topic = topicName,
        topicDlq = topicDLQName,
        subscriptionName = MainSubscription.name(topic),
        subscriptionNameDlq = MainSubscription.nameDLQ(topic),
        subscriptionType = MainSubscription.type(topic),
        consumerName = name,
        concurrency = concurrency,
    )
  }
}

