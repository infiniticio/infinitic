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
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticConsumer(private val channels: InMemoryChannels) : InfiniticConsumer {

  private val logger = KotlinLogging.logger {}

  // Coroutine scope used to receive messages
  private val consumingScope = CoroutineScope(Dispatchers.IO)

  override fun close() {
    consumingScope.cancel()
  }

  override fun startClientConsumerAsync(
    handler: suspend (ClientMessage) -> Unit,
    beforeDlq: (suspend (ClientMessage, Exception) -> Unit)?,
    clientName: ClientName
  ): CompletableFuture<Unit> {
    val channel = channels.forClient(clientName)
    logger.info { "Channel ${channel.id}: Starting Client consumer for $clientName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    beforeDlq: (suspend (TaskTagMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskTag(serviceName)
    logger.info { "Channel ${channel.id}: Starting TaskTag consumer for $serviceName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    beforeDlq: (suspend (WorkflowTagMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTag(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowTag consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startWorkflowCmdConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowCmd(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowStart consumer for $workflowName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowEngine consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting delayed WorkflowEngine consumer for $workflowName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting TaskExecutor consumer for $serviceName with concurrency = $concurrency" }
    return startAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting delayed TaskExecutor consumer for $serviceName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowTask consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, beforeDlq, channel, 1)
  }

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting delayed WorkflowTask consumer for $workflowName with concurrency = $concurrency" }
    return startDelayedAsync(handler, beforeDlq, channel, concurrency)
  }

  // start concurrent executors on a channel containing messages
  private fun <T : Message> startAsync(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    channel: Channel<T>,
    concurrency: Int
  ): CompletableFuture<Unit> = List(concurrency) {
    startAsync(handler, beforeDlq, channel)
  }.let { CompletableFuture.allOf(*it.toTypedArray()).thenApply { } }

  // start an executor on a channel containing messages
  private fun <T : Message> startAsync(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    channel: Channel<T>
  ): CompletableFuture<Unit> = consumingScope.future {
    for (message in channel) {
      try {
        logger.trace { "Channel ${channel.id}: Receiving $message" }
        handler(message)
      } catch (e: Exception) {
        logger.warn(e) { "Channel ${channel.id}: Error while processing message $message" }
        sendToDlq(beforeDlq, channel, message, e)
      }
    }
  }

  // start concurrent executors on a channel containing delayed messages
  private fun <T : Message> startDelayedAsync(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    channel: Channel<DelayedMessage<T>>,
    concurrency: Int
  ) = List(concurrency) {
    startDelayedAsync(handler, beforeDlq, channel)
  }.let { CompletableFuture.allOf(*it.toTypedArray()).thenApply { } }

  // start an executor on a channel containing delayed messages
  private fun <T : Message> startDelayedAsync(
    handler: suspend (T) -> Unit,
    beforeDlq: (suspend (T, Exception) -> Unit)?,
    channel: Channel<DelayedMessage<T>>
  ) = consumingScope.future {
    for (delayedMessage in channel) {
      try {
        delay(delayedMessage.after.long)
        logger.trace { "Channel ${channel.id}: Receiving ${delayedMessage.message}" }
        handler(delayedMessage.message)
      } catch (e: Exception) {
        logger.warn(e) { "Channel ${channel.id}: Error while processing delayed message ${delayedMessage.message}" }
        sendToDlq(beforeDlq, channel, delayedMessage.message, e)
      }
    }
  }

  // emulate sending to DLQ
  private suspend fun <T : Message> sendToDlq(
    beforeDlq: (suspend (T, Exception) -> Unit)?,
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

