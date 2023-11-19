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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.topics.ClientTopics
import io.infinitic.pulsar.topics.ServiceTopics
import io.infinitic.pulsar.topics.TopicManager
import io.infinitic.pulsar.topics.WorkflowTaskTopics
import io.infinitic.pulsar.topics.WorkflowTopics
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducer(
  private val producer: Producer,
  private val topicManager: TopicManager
) : InfiniticProducer {

  private var suggestedName: String? = null

  /**
   * Name of the sender
   * (If set, this must be done before the first message is sent to be taking into account)
   */
  override var name: String
    get() = uniqueName
    set(value) {
      suggestedName = value
    }

  /**
   * If [suggestedName] is provided, we check that no other is connected with it
   * If [suggestedName] is not provided, Pulsar will provide a unique name
   */
  private val uniqueName: String by lazy {
    producer.getName(topicManager.getNamerTopic(), suggestedName).getOrThrow()
  }

  /**
   * Below, names of producers are defined lazily to avoid creating [uniqueName] before using it
   */

  // Name of producers sending messages to workflow tag
  private val clientProducerName by lazy {
    topicManager.getProducerName(name, ClientTopics.RESPONSE)
  }

  // Name of producers sending messages to workflow tag
  private val workflowTagProducerName by lazy {
    topicManager.getProducerName(name, WorkflowTopics.TAG)
  }

  // Name of producers sending messages to workflow engine
  private val workflowEngineProducerName by lazy {
    topicManager.getProducerName(name, WorkflowTopics.ENGINE)
  }

  // Name of producers sending messages to task tag
  private val taskTagProducerName by lazy {
    topicManager.getProducerName(name, ServiceTopics.TAG)
  }

  // Name of producers sending messages to task executor
  private val taskExecutorProducerName by lazy {
    topicManager.getProducerName(name, ServiceTopics.EXECUTOR)
  }


  // Asynchronously send message to client
  override fun sendAsync(message: ClientMessage): CompletableFuture<Unit> {
    val topic =
        topicManager.initTopic(ClientTopics.RESPONSE, "${message.recipientName}").getOrThrow()

    return producer.sendAsync<ClientMessage, ClientEnvelope>(
        message, zero, topic, clientProducerName,
    )
  }

  // Asynchronously send message to Workflow Tag
  override fun sendAsync(message: WorkflowTagMessage): CompletableFuture<Unit> {
    val topic =
        topicManager.initTopic(WorkflowTopics.TAG, "${message.workflowName}").getOrThrow()

    return producer.sendAsync<WorkflowTagMessage, WorkflowTagEnvelope>(
        message, zero, topic, workflowTagProducerName, key = "${message.workflowTag}",
    )
  }

  // Asynchronously send message to Workflow Engine
  override fun sendAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val topic = if (after > 0) {
      topicManager.initTopic(WorkflowTopics.DELAY, "${message.workflowName}")
    } else {
      topicManager.initTopic(WorkflowTopics.ENGINE, "${message.workflowName}")
    }.getOrThrow()

    return producer.sendAsync<WorkflowEngineMessage, WorkflowEngineEnvelope>(
        message, after, topic, workflowEngineProducerName, key = "${message.workflowId}",
    )
  }

  // Asynchronously send message to Task Tag
  override fun sendAsync(message: TaskTagMessage): CompletableFuture<Unit> {
    val topic =
        topicManager.initTopic(ServiceTopics.TAG, "${message.serviceName}").getOrThrow()

    return producer.sendAsync<TaskTagMessage, TaskTagEnvelope>(
        message, zero, topic, taskTagProducerName, key = "${message.taskTag}",
    )
  }

  // Asynchronously send message to Task Executor
  override fun sendAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val topic = if (message.isWorkflowTask()) {
      when (message) {
        is ExecuteTask -> topicManager.initTopic(
            WorkflowTaskTopics.EXECUTOR,
            "${message.workflowName!!}",
        )
      }
    } else {
      topicManager.initTopic(ServiceTopics.EXECUTOR, "${message.serviceName}")
    }.getOrThrow()

    return producer.sendAsync<TaskExecutorMessage, TaskExecutorEnvelope>(
        message, after, topic, taskExecutorProducerName,
    )
  }

  companion object {
    private val zero = MillisDuration.ZERO
  }
}
