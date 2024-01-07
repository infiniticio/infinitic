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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticProducerAsync(private val channels: InMemoryChannels) :
  InfiniticProducerAsync {

  private val logger = KotlinLogging.logger {}

  override var name = DEFAULT_NAME

  override fun sendToClientAsync(
    message: ClientMessage
  ): CompletableFuture<Unit> = sendAsync(
      message,
      channels.forClient(message.recipientName),
  )

  override fun sendToWorkflowTagAsync(
    message: WorkflowTagMessage
  ): CompletableFuture<Unit> = sendAsync(
      message,
      channels.forWorkflowTag(message.workflowName),
  )

  override fun sendToWorkflowCmdAsync(
    message: WorkflowEngineMessage
  ): CompletableFuture<Unit> = sendAsync(
      message,
      channels.forWorkflowCmd(message.workflowName),
  )

  override fun sendToWorkflowEngineAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> = when {
    after > 0 -> sendAsync(
        DelayedMessage(message, after),
        channels.forDelayedWorkflowEngine(message.workflowName),
    )

    else -> sendAsync(
        message,
        channels.forWorkflowEngine(message.workflowName),
    )
  }

  override fun sendToWorkflowEventsAsync(
    message: WorkflowEventMessage
  ): CompletableFuture<Unit> = sendAsync(
      message,
      channels.forWorkflowEvent(message.workflowName),
  )

  override fun sendToTaskTagAsync(
    message: TaskTagMessage
  ) = sendAsync(
      message,
      channels.forTaskTag(message.serviceName),
  )

  override fun sendToTaskExecutorAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> = when {
    message.isWorkflowTask() -> when (message) {
      is ExecuteTask -> when {
        after > 0 -> sendAsync(
            DelayedMessage(message, after),
            channels.forDelayedWorkflowTaskExecutor(message.workflowName!!),
        )

        else -> sendAsync(
            message,
            channels.forWorkflowTaskExecutor(message.workflowName!!),
        )
      }

      else -> thisShouldNotHappen()
    }

    else -> when {
      after > 0 -> sendAsync(
          DelayedMessage(message, after),
          channels.forDelayedTaskExecutor(message.serviceName),
      )

      else -> sendAsync(
          message,
          channels.forTaskExecutor(message.serviceName),
      )
    }
  }

  override fun sendToTaskEventsAsync(message: TaskEventMessage): CompletableFuture<Unit> = when {
    message.isWorkflowTask() -> sendAsync(
        message,
        channels.forWorkflowTaskEvents(message.workflowName!!),
    )

    else -> sendAsync(
        message,
        channels.forTaskEvents(message.serviceName),
    )
  }

  private fun <T : Any> sendAsync(
    message: T,
    channel: Channel<T>,
  ): CompletableFuture<Unit> {
    logger.debug { "Channel ${channel.id}: sending $message" }
    val future = with(channels) { channel.sendAsync(message) }
    logger.trace { "Channel ${channel.id}: sent" }

    return future

  }

  companion object {
    private const val DEFAULT_NAME = "inMemory"
  }
}

