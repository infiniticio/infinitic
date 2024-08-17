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
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowTaskExecutorTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.TimerWorkflowStateEngineTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

class InMemoryChannels {

  private val clientChannels =
      ConcurrentHashMap<String, Channel<ClientMessage>>()
  private val serviceTagChannels =
      ConcurrentHashMap<String, Channel<ServiceTagMessage>>()
  private val workflowTagChannels =
      ConcurrentHashMap<String, Channel<WorkflowTagEngineMessage>>()
  private val serviceExecutorChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorMessage>>()
  private val serviceEventsChannels =
      ConcurrentHashMap<String, Channel<ServiceEventMessage>>()
  private val delayedServiceExecutorChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<ServiceExecutorMessage>>>()
  private val workflowCmdChannels =
      ConcurrentHashMap<String, Channel<WorkflowStateEngineMessage>>()
  private val workflowEngineChannels =
      ConcurrentHashMap<String, Channel<WorkflowStateEngineMessage>>()
  private val delayedWorkflowEngineChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<WorkflowStateEngineMessage>>>()
  private val workflowEventsChannels =
      ConcurrentHashMap<String, Channel<WorkflowEventMessage>>()
  private val workflowTaskExecutorChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorMessage>>()
  private val workflowTaskEventsChannels =
      ConcurrentHashMap<String, Channel<ServiceEventMessage>>()
  private val delayedWorkflowTaskExecutorChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<ServiceExecutorMessage>>>()

  @Suppress("UNCHECKED_CAST")
  fun <S : Message> Topic<S>.channel(entity: String): Channel<S> = when (this) {
    ClientTopic -> clientChannels.getOrPut(entity, newChannel())
    ServiceTagTopic -> serviceTagChannels.getOrPut(entity, newChannel())
    WorkflowTagEngineTopic -> workflowTagChannels.getOrPut(entity, newChannel())
    ServiceExecutorTopic -> serviceExecutorChannels.getOrPut(entity, newChannel())
    ServiceEventsTopic -> serviceEventsChannels.getOrPut(entity, newChannel())
    WorkflowCmdTopic -> workflowCmdChannels.getOrPut(entity, newChannel())
    WorkflowStateEngineTopic -> workflowEngineChannels.getOrPut(entity, newChannel())
    WorkflowEventsTopic -> workflowEventsChannels.getOrPut(entity, newChannel())
    WorkflowTaskExecutorTopic -> workflowTaskExecutorChannels.getOrPut(entity, newChannel())
    WorkflowTaskEventsTopic -> workflowTaskEventsChannels.getOrPut(entity, newChannel())
    else -> thisShouldNotHappen()
  } as Channel<S>

  @Suppress("UNCHECKED_CAST")
  fun <S : Message> Topic<S>.channelForDelayed(entity: String): Channel<DelayedMessage<S>> {
    return when (this) {
      RetryServiceExecutorTopic -> delayedServiceExecutorChannels.getOrPut(entity, newChannel())
      TimerWorkflowStateEngineTopic -> delayedWorkflowEngineChannels.getOrPut(
          entity,
          newChannel(),
      )

      RetryWorkflowTaskExecutorTopic -> delayedWorkflowTaskExecutorChannels.getOrPut(
          entity,
          newChannel(),
      )

      else -> thisShouldNotHappen()
    } as Channel<DelayedMessage<S>>
  }

  private fun <S> newChannel(): () -> Channel<S> = { Channel(Channel.UNLIMITED) }
}

internal val Channel<*>.id
  get() = System.identityHashCode(this)

data class DelayedMessage<T : Message>(
  val message: T,
  val after: MillisDuration
)
