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
import io.infinitic.common.tasks.executors.events.TaskEventEnvelope
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workflows.engine.events.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.GlobalTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicsDescription
import io.infinitic.pulsar.resources.TopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicsDescription
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducerAsync(
  private val producer: Producer,
  private val resourceManager: ResourceManager
) : InfiniticProducerAsync {

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
    val namerTopic = getTopicName("global", GlobalTopicDescription.NAMER)
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
    resourceManager.getProducerName(name, WorkflowTopicsDescription.TAG)
  }

  // Name of producers sending messages to workflow start
  private val workflowCmdProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicsDescription.CMD)
  }

  // Name of producers sending messages to workflow engine
  private val workflowEngineProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicsDescription.ENGINE)
  }

  // Name of producers sending messages to workflow engine
  private val workflowEventProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicsDescription.EVENTS)
  }

  // Name of producers sending messages to task executor
  private val workflowTaskExecutorProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicsDescription.EXECUTOR)
  }

  // Name of producers sending messages to task executor
  private val workflowTaskEventProducerName by lazy {
    resourceManager.getProducerName(name, WorkflowTopicsDescription.EXECUTOR_EVENTS)
  }

  // Name of producers sending messages to task tag
  private val taskTagProducerName by lazy {
    resourceManager.getProducerName(name, ServiceTopicsDescription.TAG)
  }

  // Name of producers sending messages to task executor
  private val taskExecutorProducerName by lazy {
    resourceManager.getProducerName(name, ServiceTopicsDescription.EXECUTOR)
  }

  // Name of producers sending messages to task executor
  private val taskEventsProducerName by lazy {
    resourceManager.getProducerName(name, ServiceTopicsDescription.EVENTS)
  }


  // Asynchronously send message to client
  override fun sendToClientAsync(message: ClientMessage): CompletableFuture<Unit> {
    val name = "${message.recipientName}"
    val desc = ClientTopicDescription.RESPONSE

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        ClientEnvelope::class, message, zero, topic, clientProducerName,
    )
  }

  // Asynchronously send message to Workflow Tag
  override fun sendToWorkflowTagAsync(message: WorkflowTagMessage): CompletableFuture<Unit> {
    val name = "${message.workflowName}"
    val desc = WorkflowTopicsDescription.TAG

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        WorkflowTagEnvelope::class,
        message,
        zero,
        topic,
        workflowTagProducerName,
        key = "${message.workflowTag}",
    )
  }

  override fun sendToWorkflowCmdAsync(message: WorkflowEngineMessage): CompletableFuture<Unit> {
    val name = "${message.workflowName}"
    val desc = WorkflowTopicsDescription.CMD

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        WorkflowEngineEnvelope::class,
        message,
        zero,
        topic,
        workflowCmdProducerName,
        key = "${message.workflowId}",
    )
  }

  // Asynchronously send message to Workflow Engine
  override fun sendToWorkflowEngineAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val name = "${message.workflowName}"
    val desc = when (after > 0) {
      true -> WorkflowTopicsDescription.ENGINE_DELAYED
      false -> WorkflowTopicsDescription.ENGINE
    }

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic' after $after: '$message'" }

    return producer.sendAsync(
        WorkflowEngineEnvelope::class,
        message,
        after,
        topic,
        workflowEngineProducerName,
        key = "${message.workflowId}",
    )
  }

  override fun sendToWorkflowEventsAsync(message: WorkflowEventMessage): CompletableFuture<Unit> {
    val name = "${message.workflowName}"
    val desc = WorkflowTopicsDescription.EVENTS

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        WorkflowEventEnvelope::class,
        message,
        zero,
        topic,
        workflowEventProducerName,
    )
  }

  // Asynchronously send message to Task Tag
  override fun sendToTaskTagAsync(message: TaskTagMessage): CompletableFuture<Unit> {
    val name = "${message.serviceName}"
    val desc = ServiceTopicsDescription.TAG

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        TaskTagEnvelope::class,
        message,
        zero,
        topic,
        taskTagProducerName,
        key = "${message.taskTag}",
    )
  }

  // Asynchronously send message to Task Executor
  override fun sendToTaskExecutorAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val name: String
    val desc: TopicDescription
    val producerName: String

    when (message.isWorkflowTask()) {
      true -> {
        name = "${message.workflowName!!}"
        desc = WorkflowTopicsDescription.EXECUTOR
        producerName = workflowTaskExecutorProducerName
      }

      false -> {
        name = "${message.serviceName}"
        desc = ServiceTopicsDescription.EXECUTOR
        producerName = taskExecutorProducerName
      }
    }

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic' after $after: '$message'" }

    return producer.sendAsync(
        TaskExecutorEnvelope::class,
        message,
        after,
        topic,
        producerName,
    )
  }

  override fun sendToTaskEventsAsync(message: TaskEventMessage): CompletableFuture<Unit> {
    val name: String
    val desc: TopicDescription
    val producerName: String

    when (message.isWorkflowTask()) {
      true -> {
        name = "${message.workflowName!!}"
        desc = WorkflowTopicsDescription.EXECUTOR_EVENTS
        producerName = workflowTaskEventProducerName
      }

      false -> {
        name = "${message.serviceName}"
        desc = ServiceTopicsDescription.EVENTS
        producerName = taskEventsProducerName
      }
    }

    val topic = getTopicName(name, desc)
    logger.debug { "Sending to topic '$topic': '$message'" }

    return producer.sendAsync(
        TaskEventEnvelope::class,
        message,
        zero,
        topic,
        producerName,
    )
  }

  private fun getTopicName(name: String, desc: TopicDescription) =
      resourceManager.getTopicName(name, desc).also {
        // create topic if needed
        resourceManager.initTopicOnce(
            topic = it,
            isPartitioned = desc.isPartitioned,
            isDelayed = desc.isDelayed,
        )
      }

  companion object {
    private val zero = MillisDuration.ZERO
  }
}
