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
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicDescription
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducer(
  private val producer: Producer,
  private val resourceManager: ResourceManager
) : InfiniticProducer {

  private var suggestedName: String? = null

  val logger = KotlinLogging.logger {}

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
    // Create namer topic if not exists
    val namerTopic = resourceManager.initNamer().getOrThrow()
    // Get unique name
    producer.getUniqueName(namerTopic, suggestedName).getOrThrow()
  }

  /**
   * Below, names of producers are defined lazily to avoid creating [uniqueName] before using it
   */

  // Name of producers sending messages to workflow tag
  private val clientProducerName by lazy {
    resourceManager.getProducerName(name, ClientTopicDescription.RESPONSE)
  }

  // Name of producers sending messages to workflow tag
  private val workflowTagProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicDescription.TAG)
  }

  // Name of producers sending messages to workflow engine
  private val workflowEngineProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicDescription.ENGINE)
  }

  // Name of producers sending messages to task tag
  private val taskTagProducerName by lazy {
    resourceManager.getProducerName(name, ServiceTopicDescription.TAG)
  }

  // Name of producers sending messages to task executor
  private val taskExecutorProducerName by lazy {
    resourceManager.getProducerName(name, ServiceTopicDescription.EXECUTOR)
  }


  // Asynchronously send message to client
  override fun sendAsync(message: ClientMessage): CompletableFuture<Unit> {
    val topic = resourceManager.initTopic(
        "${message.recipientName}", ClientTopicDescription.RESPONSE,
    ).getOrElse { return CompletableFuture.failedFuture(it) }

    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync<ClientMessage, ClientEnvelope>(
        message, zero, topic, clientProducerName,
    )
  }

  // Asynchronously send message to Workflow Tag
  override fun sendAsync(message: WorkflowTagMessage): CompletableFuture<Unit> {
    val topic =
        resourceManager.initTopic(
            "${message.workflowName}", WorkflowTopicDescription.TAG,
        ).getOrElse { return CompletableFuture.failedFuture(it) }

    logger.debug { "Sending to topic '$topic': '$message'" }

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
      resourceManager.initTopic("${message.workflowName}", WorkflowTopicDescription.ENGINE_DELAYED)
    } else {
      resourceManager.initTopic("${message.workflowName}", WorkflowTopicDescription.ENGINE)
    }.getOrElse { return CompletableFuture.failedFuture(it) }

    logger.debug { "Sending to topic '$topic' after $after: '$message'" }

    return producer.sendAsync<WorkflowEngineMessage, WorkflowEngineEnvelope>(
        message, after, topic, workflowEngineProducerName, key = "${message.workflowId}",
    )
  }

  // Asynchronously send message to Task Tag
  override fun sendAsync(message: TaskTagMessage): CompletableFuture<Unit> {
    val topic = resourceManager.initTopic("${message.serviceName}", ServiceTopicDescription.TAG)
        .getOrElse { return CompletableFuture.failedFuture(it) }

    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync<TaskTagMessage, TaskTagEnvelope>(
        message, zero, topic, taskTagProducerName, key = "${message.taskTag}",
    )
  }

  // Asynchronously send message to Task Executor
  override fun sendAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val topic = when (message) {
      is ExecuteTask -> when (message.isWorkflowTask()) {
        true -> resourceManager.initTopic(
            "${message.workflowName!!}", WorkflowTopicDescription.EXECUTOR,
        )

        false -> resourceManager.initTopic(
            "${message.serviceName}", ServiceTopicDescription.EXECUTOR,
        )
      }
    }.getOrElse { return CompletableFuture.failedFuture(it) }

    logger.debug { "Sending to topic '$topic' after $after: '$message'" }

    return producer.sendAsync<TaskExecutorMessage, TaskExecutorEnvelope>(
        message, after, topic, taskExecutorProducerName,
    )
  }

  companion object {
    private val zero = MillisDuration.ZERO
  }
}
