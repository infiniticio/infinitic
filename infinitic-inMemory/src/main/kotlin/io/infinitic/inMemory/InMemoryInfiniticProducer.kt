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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticProducer(private val channels: InMemoryChannels) : InfiniticProducer {

  private val logger = KotlinLogging.logger {}

  // Coroutine scope used to receive messages
  private val producingScope = CoroutineScope(Dispatchers.IO)

  override var name = DEFAULT_NAME

  override fun sendAsync(message: ClientMessage) = sendAsync(
      message,
      channels.forClient(),
  )

  override fun sendAsync(message: WorkflowTagMessage) = sendAsync(
      message,
      channels.forWorkflowTag(message.workflowName),
  )

  override fun sendAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ) = sendAsync(
      message,
      channels.forWorkflowEngine(message.workflowName),
      after,
  )

  override fun sendAsync(message: TaskTagMessage) = sendAsync(
      message,
      channels.forTaskTag(message.serviceName),
  )

  override fun sendAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ) = when {
    message.isWorkflowTask() -> when (message) {
      is ExecuteTask -> sendAsync(
          message,
          channels.forWorkflowTaskExecutor(message.workflowName!!),
          after,
      )
    }

    else -> sendAsync(
        message,
        channels.forTaskExecutor(message.serviceName),
        after,
    )
  }

  private fun <T : Message> sendAsync(
    message: T,
    channel: Channel<T>,
    after: MillisDuration = MillisDuration.ZERO
  ): CompletableFuture<Unit> = producingScope.future {
    logger.debug {
      val prefix = if (after > 0) "after $after ms, " else ""
      "${prefix}sending $message"
    }
    if (after > 0) delay(after.long)
    channel.send(message)
    logger.debug { "sent" }
  }

  companion object {
    private const val DEFAULT_NAME = "inMemory"
  }
}
