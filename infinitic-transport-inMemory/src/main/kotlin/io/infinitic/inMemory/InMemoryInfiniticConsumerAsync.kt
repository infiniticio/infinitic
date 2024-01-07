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
package io.infinitic.inMemory

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticConsumerAsync(private val channels: InMemoryChannels) :
  InfiniticConsumerAsync {

  override var logName: String? = null

  private val logger = KotlinLogging.logger(logName ?: this::class.java.name)
  override fun close() {
    channels.close()
  }

  override fun startClientConsumerAsync(
    handler: (ClientMessage, MillisInstant) -> Unit,
    beforeDlq: ((ClientMessage, Exception) -> Unit)?,
    clientName: ClientName
  ): CompletableFuture<Unit> {
    val channel = channels.forClient(clientName)
    logger.info { "Channel ${channel.id}: Starting client-response consumer for $clientName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startTaskTagConsumerAsync(
    handler: (TaskTagMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskTagMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskTag(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-tag consumer for $serviceName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowTagConsumerAsync(
    handler: (WorkflowTagMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowTagMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTag(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-tag consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowCmdConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowCmd(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-cmd consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowEngineConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-engine consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-engine-delay consumer for $workflowName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startWorkflowEventsConsumerAsync(
    handler: (WorkflowEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((WorkflowEventMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowEvent(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-events consumer for $workflowName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startTaskExecutorConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-executor consumer for $serviceName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startTaskEventsConsumerAsync(
    handler: (TaskEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskEventMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskEvents(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-events consumer for $serviceName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-executor-delay consumer for $serviceName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startWorkflowTaskConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-executor consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowTaskEventsConsumerAsync(
    handler: (TaskEventMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskEventMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTaskEvents(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-events consumer for $workflowName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: (TaskExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: ((TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-delay consumer for $workflowName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  // start concurrent executors on a channel containing messages
  private fun <T : Message> startAsync(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    channel: Channel<T>,
    concurrency: Int
  ): CompletableFuture<Unit> = Array(concurrency) {
    startAsync(handler, beforeDlq, channel)
  }.let {
    CompletableFuture.allOf(*it).thenApply { }
  }

  // start an executor on a channel containing messages
  private fun <T : Message> startAsync(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    channel: Channel<T>
  ): CompletableFuture<Unit> = channels.runAsync {
    try {
      for (message in channel) {
        try {
          logger.trace { "Channel ${channel.id}: Receiving $message" }
          handler(message, MillisInstant.now())
        } catch (e: Exception) {
          logger.warn(e) { "Channel ${channel.id}: Error while processing message $message" }
          sendToDlq(beforeDlq, channel, message, e)
        }
      }
    } catch (e: CancellationException) {
      // do nothing
    }
  }.thenApply { }

  // start concurrent executors on a channel containing delayed messages
  private fun <T : Message> startDelayedAsync(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    channel: Channel<DelayedMessage<T>>,
    concurrency: Int
  ) = Array(concurrency) {
    startDelayedAsync(handler, beforeDlq, channel)
  }.let {
    CompletableFuture.allOf(*it).thenApply { }
  }

  // start an executor on a channel containing delayed messages
  private fun <T : Message> startDelayedAsync(
    handler: (T, MillisInstant) -> Unit,
    beforeDlq: ((T, Exception) -> Unit)?,
    channel: Channel<DelayedMessage<T>>
  ) = channels.runAsync {
    try {
      for (delayedMessage in channel) {
        try {
          val ts = MillisInstant.now()
          delay(delayedMessage.after.long)
          logger.trace { "Channel ${channel.id}: Receiving ${delayedMessage.message}" }
          handler(delayedMessage.message, ts)
        } catch (e: Exception) {
          logger.warn(e) { "Channel ${channel.id}: Error while processing delayed message ${delayedMessage.message}" }
          sendToDlq(beforeDlq, channel, delayedMessage.message, e)
        }
      }
    } catch (e: CancellationException) {
      // do nothing
    }
  }

  // emulate sending to DLQ
  private fun <T : Message> sendToDlq(
    beforeDlq: ((T, Exception) -> Unit)?,
    channel: Channel<*>,
    message: T,
    e: Exception
  ) {
    beforeDlq?.let {
      try {
        logger.trace { "Channel ${channel.id}: Telling about message sent to DLQ $message}" }
        it(message, e)
      } catch (e: Exception) {
        logger.error(e) { "Channel ${channel.id}: Unable to tell about message sent to DLQ $message" }
      }
    }
  }
}

