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
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.topics.DelayedWorkflowServiceExecutorTopic
import io.infinitic.common.topics.ServiceEventsTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowCmdTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.topics.WorkflowServiceEventsTopic
import io.infinitic.common.topics.WorkflowServiceExecutorTopic
import io.infinitic.common.topics.WorkflowTagTopic
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.future.await

class LoggedInfiniticProducer(
  logName: String,
  private val producerAsync: InfiniticProducerAsync,
) : InfiniticProducer {

  /**
   * The `producingScope` is a Coroutine Scope used for running task, engine, etc.
   *
   * Note: Using Dispatchers.IO instead of Dispatchers.Default creates a deadlock for high
   * concurrency - see infinitic-transport-pulsar/src/test/kotlin/io/infinitic/pulsar/Test
   *
   * A SupervisorJob instance is used in the CoroutineScope to ensure that
   * if an exception occurs during the processing,
   * the scope itself is not cancelled.
   */

  private val logger = KotlinLogging.logger(logName)

  lateinit var id: String

  override var name: String
    get() = producerAsync.name
    set(value) {
      producerAsync.name = value
    }

  override suspend fun <T : Message> sendTo(
    message: T,
    topic: Topic<T>,
    after: MillisDuration?
  ) {
    TODO("Not yet implemented")
  }

  override suspend fun sendToClient(message: ClientMessage) = with(producerAsync) {
    logDebug(message)
    message.sendToAsync(ClientTopic).await()
    logTrace(message)
  }

  override suspend fun sendToWorkflowTag(message: WorkflowTagMessage) = with(producerAsync) {
    logDebug(message)
    message.sendToAsync(WorkflowTagTopic).await()
    logTrace(message)
  }

  override suspend fun sendToWorkflowCmd(message: WorkflowEngineMessage) = with(producerAsync) {
    logDebug(message)
    message.sendToAsync(WorkflowCmdTopic).await()
    logTrace(message)
  }

  override suspend fun sendToWorkflowEngine(
    message: WorkflowEngineMessage,
  ) {
    logDebug(message)
    with(producerAsync) { message.sendToAsync(WorkflowEngineTopic) }.await()
    logTrace(message)
  }

  override suspend fun sendToWorkflowEngineAfter(
    message: WorkflowEngineMessage,
    after: MillisDuration
  ) {
    logDebug(message, after)
    with(producerAsync) { message.sendToAsync(DelayedWorkflowEngineTopic, after) }.await()
    logTrace(message)
  }

  override suspend fun sendToWorkflowEvents(message: WorkflowEventMessage) = with(producerAsync) {
    logDebug(message)
    message.sendToAsync(WorkflowEventsTopic).await()
    logTrace(message)
  }

  override suspend fun sendToServiceTag(message: ServiceTagMessage) = with(producerAsync) {
    logDebug(message)
    message.sendToAsync(ServiceTagTopic).await()
    logTrace(message)
  }

  override suspend fun sendToServiceExecutor(message: ServiceExecutorMessage) =
      with(producerAsync) {
        logDebug(message)
        when (message.isWorkflowTask()) {
          true -> message.sendToAsync(WorkflowServiceExecutorTopic).await()
          false -> message.sendToAsync(ServiceExecutorTopic).await()
        }
        logTrace(message)
      }

  override suspend fun sendToServiceExecutorAfter(
    message: ServiceExecutorMessage,
    after: MillisDuration
  ) = with(producerAsync) {
    logDebug(message, after)
    when (message.isWorkflowTask()) {
      true -> when (after > 0) {
        true -> message.sendToAsync(DelayedWorkflowServiceExecutorTopic, after).await()
        false -> sendToServiceExecutor(message)
      }

      false -> when (after > 0) {
        true -> message.sendToAsync(DelayedServiceExecutorTopic, after).await()
        false -> sendToServiceExecutor(message)
      }
    }
    logTrace(message)
  }

  override suspend fun sendToTaskEvents(message: ServiceEventMessage) = with(producerAsync) {
    logDebug(message)
    when (message.isWorkflowTask()) {
      true -> message.sendToAsync(WorkflowServiceEventsTopic).await()
      false -> message.sendToAsync(ServiceEventsTopic).await()
    }
    logTrace(message)
  }

  private fun logDebug(message: Message, after: MillisDuration? = null) {
    logger.debug {
      val idStr = if (::id.isInitialized) "Id $id - " else ""
      val afterStr = if (after != null && after > 0) "After $after, s" else "S"
      "$idStr${afterStr}ending $message"
    }
  }

  private fun logTrace(message: Message) {
    logger.trace {
      val idStr = if (::id.isInitialized) "Id $id - " else ""
      "${idStr}Sent $message"
    }
  }
}
