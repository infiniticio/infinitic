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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowServiceExecutorTopic
import io.infinitic.common.topics.ServiceEventsTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.topics.WorkflowServiceEventsTopic
import io.infinitic.common.topics.WorkflowServiceExecutorTopic
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import kotlinx.coroutines.future.await

class LoggedInfiniticProducer(
  logName: String,
  private val producerAsync: InfiniticProducerAsync,
) : InfiniticProducer {
  private val logger = KotlinLogging.logger(logName)

  lateinit var id: String

  override var name: String
    get() = producerAsync.producerName
    set(value) {
      producerAsync.producerName = value
    }

  override suspend fun <T : Message> T.sendTo(
    topic: Topic<T>,
    after: MillisDuration
  ) {
    logDebug(this)
    with(producerAsync) { sendToAsync(topic, after) }.await()
    logTrace(this)
  }

//  override suspend fun sendToClient(message: ClientMessage) {
//    logDebug(message)
//    with(producerAsync) { message.sendToAsync(ClientTopic) }.await()
//    logTrace(message)
//  }

//  override suspend fun sendToWorkflowTag(message: WorkflowTagMessage) {
//    logDebug(message)
//    with(producerAsync) { message.sendToAsync(WorkflowTagTopic) }.await()
//    logTrace(message)
//  }

//  override suspend fun sendToWorkflowCmd(message: WorkflowEngineMessage) {
//    logDebug(message)
//    with(producerAsync) { message.sendToAsync(WorkflowCmdTopic) }.await()
//    logTrace(message)
//  }

//  override suspend fun sendToWorkflowEngine(
//    message: WorkflowEngineMessage,
//  ) {
//    logDebug(message)
//    with(producerAsync) { message.sendToAsync(WorkflowEngineTopic) }.await()
//    logTrace(message)
//  }

//  override suspend fun sendToWorkflowEngineAfter(
//    message: WorkflowEngineMessage,
//    after: MillisDuration
//  ) {
//    logDebug(message, after)
//    with(producerAsync) { message.sendToAsync(DelayedWorkflowEngineTopic, after) }.await()
//    logTrace(message)
//  }

  override suspend fun sendToWorkflowEvents(message: WorkflowEventMessage) {
    logDebug(message)
    with(producerAsync) { message.sendToAsync(WorkflowEventsTopic) }.await()
    logTrace(message)
  }

  override suspend fun sendToServiceTag(message: ServiceTagMessage) {
    logDebug(message)
    with(producerAsync) { message.sendToAsync(ServiceTagTopic) }.await()
    logTrace(message)
  }

  override suspend fun sendToServiceExecutor(message: ServiceExecutorMessage) {
    logDebug(message)
    with(producerAsync) {
      when (message.isWorkflowTask()) {
        true -> message.sendToAsync(WorkflowServiceExecutorTopic)
        false -> message.sendToAsync(ServiceExecutorTopic)
      }
    }.await()
    logTrace(message)
  }

  override suspend fun sendToServiceExecutorAfter(
    message: ServiceExecutorMessage,
    after: MillisDuration
  ) {
    logDebug(message, after)

    when (message.isWorkflowTask()) {
      true -> when (after > 0) {
        true -> with(producerAsync) {
          message.sendToAsync(DelayedWorkflowServiceExecutorTopic, after)
        }.await()

        false -> sendToServiceExecutor(message)
      }

      false -> when (after > 0) {
        true -> with(producerAsync) {
          message.sendToAsync(DelayedServiceExecutorTopic, after)
        }.await()

        false -> sendToServiceExecutor(message)
      }
    }
    logTrace(message)
  }

  override suspend fun sendToTaskEvents(message: ServiceEventMessage) {
    logDebug(message)
    with(producerAsync) {
      when (message.isWorkflowTask()) {
        true -> message.sendToAsync(WorkflowServiceEventsTopic)
        false -> message.sendToAsync(ServiceEventsTopic)
      }
    }.await()
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
