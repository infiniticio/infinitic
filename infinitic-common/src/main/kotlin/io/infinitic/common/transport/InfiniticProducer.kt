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
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.Topic
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage

interface InfiniticProducer {
  /**
   * Name of the sender
   */
  var name: String

  suspend fun <T : Message> sendTo(
    message: T,
    topic: Topic<T>,
    after: MillisDuration? = null
  )

  /**
   * Synchronously send a message to a client
   *
   * @param message the message to send
   */
  suspend fun sendToClient(message: ClientMessage)

  /**
   * Synchronously send a message to a workflow tag engine
   *
   * @param message the message to send
   */
  suspend fun sendToWorkflowTag(message: WorkflowTagMessage)

  /**
   * Synchronously send a message to a workflow-cmd
   *
   * @param message the message to send
   */
  suspend fun sendToWorkflowCmd(message: WorkflowEngineMessage)

  /**
   * Synchronously send a message to a workflow-engine
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  suspend fun sendToWorkflowEngine(message: WorkflowEngineMessage)

  suspend fun sendToWorkflowEngineAfter(
    message: WorkflowEngineMessage,
    after: MillisDuration
  )

  /**
   * Synchronously send a message to workflow-events
   *
   * @param message the message to send
   */
  suspend fun sendToWorkflowEvents(message: WorkflowEventMessage)

  /**
   * Synchronously send a message to a task-tag
   *
   * @param message the message to send
   */
  suspend fun sendToServiceTag(message: ServiceTagMessage)

  /**
   * Synchronously send a message to a task-executor
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  suspend fun sendToServiceExecutor(
    message: ServiceExecutorMessage
  )

  /**
   * Synchronously send a message to a task-executor
   *
   * @param message the message to send
   * @param after the delay before sending the message
   */
  suspend fun sendToServiceExecutorAfter(
    message: ServiceExecutorMessage,
    after: MillisDuration
  )


  /**
   * Synchronously send a message to task-events
   *
   * @param message the message to send to the task result handler.
   */
  suspend fun sendToTaskEvents(
    message: ServiceEventMessage
  )
}
