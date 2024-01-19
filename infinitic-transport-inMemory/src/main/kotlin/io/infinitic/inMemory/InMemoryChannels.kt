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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class InMemoryChannels : AutoCloseable {

  // Coroutine scope used to receive messages
  private val consumingScope = CoroutineScope(Dispatchers.IO)
  suspend fun <T> consume(func: suspend () -> T) = coroutineScope {
    launch(consumingScope.coroutineContext) { func() }
  }

  override fun close() {
    consumingScope.cancel()
    runBlocking { consumingScope.coroutineContext.job.children.forEach { it.join() } }
  }

  // Client channel
  private val clientChannels =
      ConcurrentHashMap<ClientName, Channel<ClientMessage>>()

  // Channel for TaskTagMessages
  private val taskTagChannels =
      ConcurrentHashMap<ServiceName, Channel<ServiceTagMessage>>()

  // Channel for WorkflowTagMessages
  private val workflowTagChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessage>>()

  // Channel for TaskExecutorMessages
  private val taskExecutorChannels =
      ConcurrentHashMap<ServiceName, Channel<ServiceExecutorMessage>>()

  // Channel for TaskEventsMessages
  private val taskEventsChannels =
      ConcurrentHashMap<ServiceName, Channel<ServiceEventMessage>>()

  // Channel for delayed TaskExecutorMessages
  private val delayedTaskExecutorChannels =
      ConcurrentHashMap<ServiceName, Channel<DelayedMessage<ServiceExecutorMessage>>>()

  // Channel for WorkflowStart
  private val workflowCmdChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()

  // Channel for WorkflowEngineMessages
  private val workflowEngineChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()

  // Channel for delayed WorkflowEngineMessages
  private val delayedWorkflowEngineChannels =
      ConcurrentHashMap<WorkflowName, Channel<DelayedMessage<WorkflowEngineMessage>>>()

  // Channel for WorkflowEventMessages
  private val workflowEventChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEventMessage>>()

  // Channel for WorkflowTaskMessages
  private val workflowTaskExecutorChannels =
      ConcurrentHashMap<WorkflowName, Channel<ServiceExecutorMessage>>()

  // Channel for WorkflowTaskMessages
  private val workflowTaskEventsChannels =
      ConcurrentHashMap<WorkflowName, Channel<ServiceEventMessage>>()

  // Channel for delayed WorkflowTaskMessages
  private val delayedWorkflowTaskExecutorChannels =
      ConcurrentHashMap<WorkflowName, Channel<DelayedMessage<ServiceExecutorMessage>>>()

  fun forClient(clientName: ClientName): Channel<ClientMessage> =
      clientChannels.getOrPut(clientName) { Channel(Channel.UNLIMITED) }

  fun forTaskTag(serviceName: ServiceName): Channel<ServiceTagMessage> =
      taskTagChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTag(workflowName: WorkflowName): Channel<WorkflowTagMessage> =
      workflowTagChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forTaskExecutor(serviceName: ServiceName): Channel<ServiceExecutorMessage> =
      taskExecutorChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forTaskEvents(serviceName: ServiceName): Channel<ServiceEventMessage> =
      taskEventsChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forDelayedTaskExecutor(serviceName: ServiceName): Channel<DelayedMessage<ServiceExecutorMessage>> =
      delayedTaskExecutorChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowCmd(workflowName: WorkflowName): Channel<WorkflowEngineMessage> =
      workflowCmdChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowEngine(workflowName: WorkflowName): Channel<WorkflowEngineMessage> =
      workflowEngineChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forDelayedWorkflowEngine(workflowName: WorkflowName): Channel<DelayedMessage<WorkflowEngineMessage>> =
      delayedWorkflowEngineChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowEvent(workflowName: WorkflowName): Channel<WorkflowEventMessage> =
      workflowEventChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTaskExecutor(workflowName: WorkflowName): Channel<ServiceExecutorMessage> =
      workflowTaskExecutorChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTaskEvents(workflowName: WorkflowName): Channel<ServiceEventMessage> =
      workflowTaskEventsChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forDelayedWorkflowTaskExecutor(workflowName: WorkflowName): Channel<DelayedMessage<ServiceExecutorMessage>> =
      delayedWorkflowTaskExecutorChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

}

internal val Channel<*>.id
  get() = System.identityHashCode(this)

data class DelayedMessage<T : Message>(
  val message: T,
  val after: MillisDuration
)
