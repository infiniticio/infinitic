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
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
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
    clientName: ClientName
  ): CompletableFuture<Unit> {
    val channel = channels.forClient()
    logger.info { "Channel ${channel.id}: Starting Client consumer for $clientName with concurrency = 1" }
    return startAsync(handler, channel)
  }

  override fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskTag(serviceName)
    logger.info { "Channel ${channel.id}: Starting TaskTag consumer for $serviceName with concurrency = 1" }
    return startAsync(handler, channel, 1)
  }

  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTag(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowTag consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, channel, 1)
  }

  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowEngine consumer for $workflowName with concurrency = 1" }
    return startAsync(handler, channel, 1)
  }

  override fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting delayed WorkflowEngine consumer for $workflowName with concurrency = $concurrency" }
    return startAsync(getDelayedHandler(handler), channel, concurrency)
  }

  override fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting TaskExecutor consumer for $serviceName with concurrency = $concurrency" }
    return startAsync(handler, channel, concurrency)
  }

  override fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting delayed TaskExecutor consumer for $serviceName with concurrency = $concurrency" }
    return startAsync(getDelayedHandler(handler), channel, concurrency)
  }

  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting WorkflowTask consumer for $workflowName with concurrency = $1" }
    return startAsync(handler, channel, 1)
  }

  override fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    val channel = channels.forDelayedWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting delayed WorkflowTask consumer for $workflowName with concurrency = $concurrency" }
    return startAsync(getDelayedHandler(handler), channel, concurrency)
  }

  private fun <T : Message> getDelayedHandler(handler: suspend (T) -> Unit): suspend (DelayedMessage<T>) -> Unit =
      { delayedMessage: DelayedMessage<T> ->
        run {
          delay(delayedMessage.after.long)
          handler(delayedMessage.message)
        }
      }

  private fun <T : Any> startAsync(
    handler: suspend (T) -> Unit,
    channel: Channel<T>,
    concurrency: Int
  ): CompletableFuture<Unit> = (1..concurrency).map { startAsync(handler, channel) }.let {
    CompletableFuture.allOf(*it.toTypedArray()).thenApply { }
  }

  private fun <T : Any> startAsync(
    executor: suspend (T) -> Unit,
    channel: Channel<T>
  ): CompletableFuture<Unit> = consumingScope.future {
    for (message in channel) {
      try {
        logger.debug { "Channel ${channel.id}: receiving $message" }
        executor(message)
        logger.debug { "Channel ${channel.id}: processed ${message::class}" }
      } catch (e: Throwable) {
        logger.error(e) { "Error while processing message $message" }
      }
    }
  }
}
