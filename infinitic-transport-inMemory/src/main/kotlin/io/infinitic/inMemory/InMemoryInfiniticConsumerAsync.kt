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
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.Topic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class InMemoryInfiniticConsumerAsync(private val channels: InMemoryChannels) :
  InfiniticConsumerAsync {

  override var logName: String? = null

  override fun join() {
    //
  }

  override suspend fun <S : Message> startConsumerAsync(
    topic: Topic<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    entity: String
  ) {
    TODO("Not yet implemented")
  }

  private val logger = KotlinLogging.logger(logName ?: this::class.java.name)
  override fun close() {
    channels.close()
  }

  override suspend fun startClientConsumerAsync(
    handler: suspend (ClientMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ClientMessage?, Exception) -> Unit,
    clientName: ClientName
  ) {
    val channel = channels.forClient(clientName)
    logger.info { "Channel ${channel.id}: Starting client-response consumer for $clientName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startTaskTagConsumerAsync(
    handler: suspend (ServiceTagMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceTagMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) {
    val channel = channels.forTaskTag(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-tag consumer for $serviceName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowTagMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowTag(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-tag consumer for $workflowName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startWorkflowCmdConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowCmd(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-cmd consumer for $workflowName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-engine consumer for $workflowName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forDelayedWorkflowEngine(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-engine-delay consumer for $workflowName with concurrency = $concurrency" }
    return startLoopForDelayed(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startWorkflowEventsConsumerAsync(
    handler: suspend (WorkflowEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEventMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowEvent(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-events consumer for $workflowName with concurrency = $concurrency" }
    return startLoop(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) {
    val channel = channels.forTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-executor consumer for $serviceName with concurrency = $concurrency" }
    return startLoop(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceEventMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) {
    val channel = channels.forTaskEvents(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-events consumer for $serviceName with concurrency = $concurrency" }
    return startLoop(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    serviceName: ServiceName,
    concurrency: Int
  ) {
    val channel = channels.forDelayedTaskExecutor(serviceName)
    logger.info { "Channel ${channel.id}: Starting task-executor-delay consumer for $serviceName with concurrency = $concurrency" }
    return startLoopForDelayed(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-executor consumer for $workflowName with concurrency = 1" }
    return startLoop(handler, beforeDlq, channel, 1)
  }

  override suspend fun startWorkflowTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceEventMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forWorkflowTaskEvents(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-events consumer for $workflowName with concurrency = $concurrency" }
    return startLoop(handler, beforeDlq, channel, concurrency)
  }

  override suspend fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val channel = channels.forDelayedWorkflowTaskExecutor(workflowName)
    logger.info { "Channel ${channel.id}: Starting workflow-task-delay consumer for $workflowName with concurrency = $concurrency" }
    return startLoopForDelayed(handler, beforeDlq, channel, concurrency)
  }

  // start concurrent executors on a channel containing messages
  private suspend fun <T : Message> startLoop(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<T>,
    concurrency: Int
  ) = repeat(concurrency) {
    startLoop(handler, beforeDlq, channel)
  }

  // start an executor on a channel containing messages
  private suspend fun <T : Message> startLoop(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<T>
  ) = channels.consume {
    try {
      for (message in channel) {
        try {
          logger.trace { "Channel ${channel.id}: Receiving $message" }
          handler(message, MillisInstant.now())
        } catch (e: Exception) {
          logger.warn(e) { "Channel ${channel.id}: Error while processing message $message" }
          sendToDlq(beforeDlq, channel, message, e)
        }
      }
    } catch (e: CancellationException) {
      logger.info { "Channel ${channel.id} Canceled" }
    }
  }

  // start concurrent executors on a channel containing delayed messages
  private suspend fun <T : Message> startLoopForDelayed(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<DelayedMessage<T>>,
    concurrency: Int
  ) = repeat(concurrency) {
    startLoopForDelayed(handler, beforeDlq, channel)
  }

  // start an executor on a channel containing delayed messages
  private suspend fun <T : Message> startLoopForDelayed(
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<DelayedMessage<T>>
  ) = channels.consume {
    try {
      for (delayedMessage in channel) {
        try {
          val ts = MillisInstant.now()
          delay(delayedMessage.after.long)
          logger.trace { "Channel ${channel.id}: Receiving ${delayedMessage.message}" }
          handler(delayedMessage.message, ts)
        } catch (e: Exception) {
          logger.warn(e) { "Channel ${channel.id}: Error while processing delayed message ${delayedMessage.message}" }
          sendToDlq(beforeDlq, channel, delayedMessage.message, e)
        }
      }
    } catch (e: CancellationException) {
      logger.info { "Channel ${channel.id} Canceled" }
    }
  }

  // emulate sending to DLQ
  private suspend fun <T : Message> sendToDlq(
    beforeDlq: suspend (T?, Exception) -> Unit,
    channel: Channel<*>,
    message: T,
    e: Exception
  ) {
    try {
      logger.trace { "Channel ${channel.id}: Telling about message sent to DLQ $message}" }
      beforeDlq(message, e)
    } catch (e: Exception) {
      logger.error(e) { "Channel ${channel.id}: Unable to tell about message sent to DLQ $message" }
    }
  }
}

