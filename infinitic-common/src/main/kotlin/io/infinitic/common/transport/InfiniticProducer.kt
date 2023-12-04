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

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

interface InfiniticProducer {
  /**
   * Name of the sender
   */
  var name: String

  /**
   * Asynchronously send a message to a client
   *
   * @param message the message to send
   */
  fun sendAsync(message: ClientMessage): CompletableFuture<Unit>

  /**
   * Asynchronously send a message to a workflow tag engine
   *
   * @param message the message to send
   */
  fun sendAsync(message: WorkflowTagMessage): CompletableFuture<Unit>

  /**
   * Asynchronously send a message to a workflow engine
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  fun sendAsync(
    message: WorkflowEngineMessage,
    after: MillisDuration = MillisDuration.ZERO
  ): CompletableFuture<Unit>

  /**
   * Asynchronously send a message to a task tag engine
   *
   * @param message the message to send
   */
  fun sendAsync(message: TaskTagMessage): CompletableFuture<Unit>

  /**
   * Asynchronously send a message to a task executor
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  fun sendAsync(
    message: TaskExecutorMessage,
    after: MillisDuration = MillisDuration.ZERO
  ): CompletableFuture<Unit>

  /**
   * Synchronously send a message to a client
   *
   * @param message the message to send
   */
  suspend fun send(message: ClientMessage): Unit = sendAsync(message).await()

  /**
   * Synchronously send a message to a workflow tag engine
   *
   * @param message the message to send
   */
  suspend fun send(message: WorkflowTagMessage): Unit = sendAsync(message).await()

  /**
   * Synchronously send a message to a workflow engine
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  suspend fun send(
    message: WorkflowEngineMessage,
    after: MillisDuration = MillisDuration.ZERO
  ): Unit = sendAsync(message, after).await()

  /**
   * Synchronously send a message to a task tag engine
   *
   * @param message the message to send
   */
  suspend fun send(message: TaskTagMessage): Unit = sendAsync(message).await()

  /**
   * Synchronously send a message to a task executor
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  suspend fun send(
    message: TaskExecutorMessage,
    after: MillisDuration = MillisDuration.ZERO
  ): Unit = sendAsync(message, after).await()
}
