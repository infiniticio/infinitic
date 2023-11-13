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

import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
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
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.consumers.ConsumerConfig
import io.infinitic.pulsar.producers.getProducerName
import io.infinitic.pulsar.topics.ClientTopics
import io.infinitic.pulsar.topics.GlobalTopics
import io.infinitic.pulsar.topics.ServiceTopics
import io.infinitic.pulsar.topics.TopicNames
import io.infinitic.pulsar.topics.TopicNamesDefault
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.WorkflowTaskTopics
import io.infinitic.pulsar.topics.WorkflowTopics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import mu.KotlinLogging
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.api.PulsarClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class PulsarInfiniticConsumer(
  private val pulsarClient: PulsarClient,
  private val pulsarAdmin: PulsarAdmin,
  private val consumerConfig: ConsumerConfig,
  private val topicNames: TopicNames
) : InfiniticConsumer {

  private val logger = KotlinLogging.logger {}

  private val consumer = Consumer(pulsarClient, consumerConfig)

  /** Set of client topics created */
  private val clientTopics: MutableSet<String> = ConcurrentHashMap.newKeySet()

  /** Coroutine scope used to receive messages */
  private val consumingScope = CoroutineScope(Dispatchers.IO)


  override fun close() {
    consumingScope.cancel()
    clientTopics.forEach { topic: String ->
      try {
        logger.debug { "Deleting client topic $topic" }
        pulsarAdmin.topics().delete(topic, true)
      } catch (e: Exception) {
        logger.warn { "Error while deleting client topic $topic: $e" }
      }
    }
  }

  private fun createClientTopic(topic: String) {
    try {
      logger.debug { "Creating of client topic $topic" }
      pulsarAdmin.topics().createNonPartitionedTopic(topic)
      clientTopics.add(topic)
    } catch (e: PulsarAdminException.ConflictException) {
      logger.debug { "Client topic already exists: $topic: ${e.message}" }
    } catch (e: PulsarAdminException.NotAllowedException) {
      logger.warn { "Not allowed to create client topic $topic: ${e.message}" }
    } catch (e: PulsarAdminException.NotAuthorizedException) {
      logger.warn { "Not authorized to create client topic $topic: ${e.message}" }
    } catch (e: Exception) {
      logger.warn(e) {}
    }
  }

  // Start consumers of messages to client
  override fun startClientConsumerAsync(
    handler: suspend (ClientMessage) -> Unit,
    name: String?
  ): CompletableFuture<Unit> {
    // Check name uniqueness if provided or create one
    val clientName = getProducerName(pulsarClient, topicNames.topic(GlobalTopics.NAMER), name)
    // Manage client topic
    createClientTopic(topicNames.topic(ClientTopics.RESPONSE, clientName))

    return startAsync<ClientMessage, ClientEnvelope>(
        handler = handler,
        topicType = ClientTopics.RESPONSE,
        concurrency = 1,
        name = clientName,
    )
  }

  // Start consumers of messages to workflow tag
  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<WorkflowTagMessage, WorkflowTagEnvelope>(
      handler = handler,
      topicType = WorkflowTopics.TAG,
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
      topicType = WorkflowTopics.ENGINE,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start consumers of delayed messages to workflow engine
  override fun startWorkflowDelayConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<WorkflowEngineMessage, WorkflowEngineEnvelope>(
      handler = handler,
      topicType = WorkflowTopics.DELAY,
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
      topicType = ServiceTopics.TAG,
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
      topicType = ServiceTopics.EXECUTOR,
      concurrency = concurrency,
      name = "$serviceName",
  )

  // Start consumers of messages to workflow task executor
  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) = startAsync<TaskExecutorMessage, TaskExecutorEnvelope>(
      handler = handler,
      topicType = WorkflowTaskTopics.EXECUTOR,
      concurrency = concurrency,
      name = "$workflowName",
  )

  // Start a consumer on a topic, with concurrent executors
  private inline fun <T : Message, reified S : Envelope<T>> startAsync(
    noinline handler: suspend (T) -> Unit, topicType: TopicType, concurrency: Int, name: String
  ): CompletableFuture<Unit> = with(consumer) {
    consumingScope.future {
      startConsumer<T, S>(
          handler = handler,
          topic = topicNames.topic(topicType, name),
          subscriptionName = topicType.subscriptionName,
          subscriptionType = topicType.subscriptionType,
          consumerName = topicNames.consumerName(name, topicType),
          concurrency = concurrency,
          topicDLQ = topicNames.topicDLQ(topicType, name),
      )
    }
  }

  companion object {
    /** Create PulsarInfiniticListener from the configuration file */
    @JvmStatic
    fun from(pulsar: Pulsar) =
        PulsarInfiniticConsumer(
            pulsar.client,
            pulsar.admin,
            pulsar.consumer,
            TopicNamesDefault(pulsar.tenant, pulsar.namespace),
        )
  }
}
