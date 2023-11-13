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
import kotlinx.coroutines.future.future
import mu.KotlinLogging
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
    name: String?
  ): CompletableFuture<Unit> = startAsync(handler, channels.forClient())

  override fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> = startAsync(
      handler,
      channels.forWorkflowTag(workflowName),
      1,
  )

  override fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> = startAsync(
      handler,
      channels.forWorkflowEngine(workflowName),
      1,
  )

  override fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> = startAsync(
      handler,
      channels.forTaskTag(serviceName),
      1,
  )

  override fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit> = startAsync(
      handler,
      channels.forTaskExecutor(serviceName),
      concurrency,
  )

  override fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> = startAsync(
      handler,
      channels.forWorkflowTaskExecutor(workflowName),
      concurrency,
  )

  override fun startWorkflowDelayConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit> {
    TODO("Not Needed")
  }

  private fun <T : Message> startAsync(
    handler: suspend (T) -> Unit,
    channel: Channel<T>,
    concurrency: Int
  ): CompletableFuture<Unit> = (1..concurrency).map { startAsync(handler, channel) }.let {
    CompletableFuture.allOf(*it.toTypedArray()).thenApply { }
  }

  private fun <T : Message> startAsync(
    executor: suspend (T) -> Unit,
    channel: Channel<T>
  ): CompletableFuture<Unit> = consumingScope.future {
    for (message in channel) {
      try {
        executor(message)
      } catch (e: Throwable) {
        logger.error(e) { "Error while processing message $message" }
      }
    }
  }
}
