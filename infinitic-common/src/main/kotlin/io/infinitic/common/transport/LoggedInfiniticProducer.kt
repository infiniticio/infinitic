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
package io.infinitic.common.transport

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import java.util.concurrent.CompletableFuture

class LoggedInfiniticProducer(
  private val logName: String,
  private val producer: InfiniticProducer,
) : InfiniticProducer {

  private val logger = KotlinLogging.logger(logName)

  lateinit var id: String

  override var name: String
    get() = producer.name
    set(value) {
      producer.name = value
    }

  override fun sendAsync(message: ClientMessage): CompletableFuture<Unit> {
    logDebug(message)
    return producer.sendAsync(message)
  }

  override fun sendAsync(message: WorkflowTagMessage): CompletableFuture<Unit> {
    logDebug(message)
    return producer.sendAsync(message)
  }

  override fun sendAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    logDebug(message, after)
    return producer.sendAsync(message, after)
  }

  override fun sendAsync(message: TaskTagMessage): CompletableFuture<Unit> {
    logDebug(message)
    return producer.sendAsync(message)
  }

  override fun sendAsync(
    message: TaskExecutorMessage,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    logDebug(message, after)
    return producer.sendAsync(message, after)
  }

  private fun logDebug(message: Message, after: MillisDuration? = null) {
    logger.debug { "Id $id - ${if (after != null && after > 0) "After $after, s" else "S"}ending $message" }
  }
}
