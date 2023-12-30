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
import io.infinitic.common.tasks.executors.events.TaskEventMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

class InMemoryChannels : AutoCloseable {

  override fun close() {
    clientChannels.values.forEach { it.close() }
    taskTagChannels.values.forEach { it.close() }
    workflowTagChannels.values.forEach { it.close() }
    taskExecutorChannels.values.forEach { it.close() }
    taskEventsChannels.values.forEach { it.close() }
    delayedTaskExecutorChannels.values.forEach { it.close() }
    workflowCmdChannels.values.forEach { it.close() }
    workflowEngineChannels.values.forEach { it.close() }
    delayedWorkflowEngineChannels.values.forEach { it.close() }
    workflowTaskExecutorChannels.values.forEach { it.close() }
    workflowTaskEventsChannels.values.forEach { it.close() }
    delayedWorkflowTaskExecutorChannels.values.forEach { it.close() }
  }

  // Client channel
  private val clientChannels =
      ConcurrentHashMap<ClientName, Channel<ClientMessage>>()

  // Channel for TaskTagMessages
  private val taskTagChannels =
      ConcurrentHashMap<ServiceName, Channel<TaskTagMessage>>()

  // Channel for WorkflowTagMessages
  private val workflowTagChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessage>>()

  // Channel for TaskExecutorMessages
  private val taskExecutorChannels =
      ConcurrentHashMap<ServiceName, Channel<TaskExecutorMessage>>()

  // Channel for TaskEventsMessages
  private val taskEventsChannels =
      ConcurrentHashMap<ServiceName, Channel<TaskEventMessage>>()

  // Channel for delayed TaskExecutorMessages
  private val delayedTaskExecutorChannels =
      ConcurrentHashMap<ServiceName, Channel<DelayedMessage<TaskExecutorMessage>>>()

  // Channel for WorkflowStart
  private val workflowCmdChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()

  // Channel for WorkflowEngineMessages
  private val workflowEngineChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()

  // Channel for delayed WorkflowEngineMessages
  private val delayedWorkflowEngineChannels =
      ConcurrentHashMap<WorkflowName, Channel<DelayedMessage<WorkflowEngineMessage>>>()

  // Channel for WorkflowTaskMessages
  private val workflowTaskExecutorChannels =
      ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessage>>()

  // Channel for WorkflowTaskMessages
  private val workflowTaskEventsChannels =
      ConcurrentHashMap<WorkflowName, Channel<TaskEventMessage>>()

  // Channel for delayed WorkflowTaskMessages
  private val delayedWorkflowTaskExecutorChannels =
      ConcurrentHashMap<WorkflowName, Channel<DelayedMessage<TaskExecutorMessage>>>()

  fun forClient(clientName: ClientName): Channel<ClientMessage> =
      clientChannels.getOrPut(clientName) { Channel(Channel.UNLIMITED) }

  fun forTaskTag(serviceName: ServiceName): Channel<TaskTagMessage> =
      taskTagChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTag(workflowName: WorkflowName): Channel<WorkflowTagMessage> =
      workflowTagChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forTaskExecutor(serviceName: ServiceName): Channel<TaskExecutorMessage> =
      taskExecutorChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forTaskEvents(serviceName: ServiceName): Channel<TaskEventMessage> =
      taskEventsChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forDelayedTaskExecutor(serviceName: ServiceName): Channel<DelayedMessage<TaskExecutorMessage>> =
      delayedTaskExecutorChannels.getOrPut(serviceName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowCmd(workflowName: WorkflowName): Channel<WorkflowEngineMessage> =
      workflowCmdChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowEngine(workflowName: WorkflowName): Channel<WorkflowEngineMessage> =
      workflowEngineChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forDelayedWorkflowEngine(workflowName: WorkflowName): Channel<DelayedMessage<WorkflowEngineMessage>> =
      delayedWorkflowEngineChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTaskExecutor(workflowName: WorkflowName): Channel<TaskExecutorMessage> =
      workflowTaskExecutorChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forWorkflowTaskEvents(workflowName: WorkflowName): Channel<TaskEventMessage> =
      workflowTaskEventsChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

  fun forDelayedWorkflowTaskExecutor(workflowName: WorkflowName): Channel<DelayedMessage<TaskExecutorMessage>> =
      delayedWorkflowTaskExecutorChannels.getOrPut(workflowName) { Channel(Channel.UNLIMITED) }

}

internal val Channel<*>.id
  get() = System.identityHashCode(this)

data class DelayedMessage<T : Message>(
  val message: T,
  val after: MillisDuration
)
