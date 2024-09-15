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
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowExecutorTopic
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

class InMemoryChannels {

  internal val clientChannels =
      ConcurrentHashMap<String, Channel<ClientMessage>>(100)
  internal val serviceTagEngineChannels =
      ConcurrentHashMap<String, Channel<ServiceTagMessage>>(100)
  internal val workflowTagEngineChannels =
      ConcurrentHashMap<String, Channel<WorkflowTagEngineMessage>>(100)
  internal val serviceExecutorChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorMessage>>(100)
  internal val serviceExecutorEventsChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorEventMessage>>(100)
  internal val retryServiceExecutorChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<ServiceExecutorMessage>>>(100)
  internal val workflowStateCmdChannels =
      ConcurrentHashMap<String, Channel<WorkflowStateEngineMessage>>(100)
  internal val workflowStateEngineChannels =
      ConcurrentHashMap<String, Channel<WorkflowStateEngineMessage>>(100)
  internal val workflowStateTimerChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<WorkflowStateEngineMessage>>>(100)
  internal val workflowStateEventChannels =
      ConcurrentHashMap<String, Channel<WorkflowStateEngineEventMessage>>(100)
  internal val workflowExecutorChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorMessage>>(100)
  internal val workflowExecutorEventChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorEventMessage>>(100)
  internal val retryWorkflowExecutorChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<ServiceExecutorMessage>>>(100)

  @Suppress("UNCHECKED_CAST")
  fun <S : Message> Topic<S>.channel(entity: String): Channel<S> = when (this) {
    ClientTopic -> clientChannels.getOrPut(entity, newChannel())
    ServiceTagEngineTopic -> serviceTagEngineChannels.getOrPut(entity, newChannel())
    WorkflowTagEngineTopic -> workflowTagEngineChannels.getOrPut(entity, newChannel())
    ServiceExecutorTopic -> serviceExecutorChannels.getOrPut(entity, newChannel())
    ServiceExecutorEventTopic -> serviceExecutorEventsChannels.getOrPut(entity, newChannel())
    WorkflowStateCmdTopic -> workflowStateCmdChannels.getOrPut(entity, newChannel())
    WorkflowStateEngineTopic -> workflowStateEngineChannels.getOrPut(entity, newChannel())
    WorkflowStateEventTopic -> workflowStateEventChannels.getOrPut(entity, newChannel())
    WorkflowExecutorTopic -> workflowExecutorChannels.getOrPut(entity, newChannel())
    WorkflowExecutorEventTopic -> workflowExecutorEventChannels.getOrPut(entity, newChannel())
    else -> thisShouldNotHappen()
  } as Channel<S>

  @Suppress("UNCHECKED_CAST")
  fun <S : Message> Topic<S>.channelForDelayed(entity: String): Channel<DelayedMessage<S>> {
    return when (this) {
      RetryServiceExecutorTopic -> retryServiceExecutorChannels.getOrPut(entity, newChannel())
      WorkflowStateTimerTopic -> workflowStateTimerChannels.getOrPut(
          entity,
          newChannel(),
      )

      RetryWorkflowExecutorTopic -> retryWorkflowExecutorChannels.getOrPut(
          entity,
          newChannel(),
      )

      else -> thisShouldNotHappen()
    } as Channel<DelayedMessage<S>>
  }

  private fun <S> newChannel(): () -> Channel<S> = { Channel(10000) }
}

internal val Channel<*>.id
  get() = System.identityHashCode(this)

data class DelayedMessage<T : Message>(
  val message: T,
  val after: MillisDuration
)
