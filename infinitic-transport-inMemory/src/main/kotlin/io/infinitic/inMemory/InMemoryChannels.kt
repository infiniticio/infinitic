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
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.topics.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.topics.ServiceEventsTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowCmdTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.topics.WorkflowTagTopic
import io.infinitic.common.topics.WorkflowTaskEventsTopic
import io.infinitic.common.topics.WorkflowTaskExecutorTopic
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

  private val clientChannels =
      ConcurrentHashMap<String, Channel<ClientMessage>>()
  private val serviceTagChannels =
      ConcurrentHashMap<String, Channel<ServiceTagMessage>>()
  private val workflowTagChannels =
      ConcurrentHashMap<String, Channel<WorkflowTagMessage>>()
  private val serviceExecutorChannels =
      ConcurrentHashMap<String, Channel<ServiceExecutorMessage>>()
  private val serviceEventsChannels =
      ConcurrentHashMap<String, Channel<ServiceEventMessage>>()
  private val delayedServiceExecutorChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<ServiceExecutorMessage>>>()
  private val workflowCmdChannels =
      ConcurrentHashMap<String, Channel<WorkflowEngineMessage>>()
  private val workflowEngineChannels =
      ConcurrentHashMap<String, Channel<WorkflowEngineMessage>>()
  private val delayedWorkflowEngineChannels =
      ConcurrentHashMap<String, Channel<DelayedMessage<WorkflowEngineMessage>>>()
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
    WorkflowTagTopic -> workflowTagChannels.getOrPut(entity, newChannel())
    ServiceExecutorTopic -> serviceExecutorChannels.getOrPut(entity, newChannel())
    ServiceEventsTopic -> serviceEventsChannels.getOrPut(entity, newChannel())
    WorkflowCmdTopic -> workflowCmdChannels.getOrPut(entity, newChannel())
    WorkflowEngineTopic -> workflowEngineChannels.getOrPut(entity, newChannel())
    WorkflowEventsTopic -> workflowEventsChannels.getOrPut(entity, newChannel())
    WorkflowTaskExecutorTopic -> workflowTaskExecutorChannels.getOrPut(entity, newChannel())
    WorkflowTaskEventsTopic -> workflowTaskEventsChannels.getOrPut(entity, newChannel())
    else -> thisShouldNotHappen()
  } as Channel<S>

  @Suppress("UNCHECKED_CAST")
  fun <S : Message> Topic<S>.channelForDelayed(entity: String): Channel<DelayedMessage<S>> {
    return when (this) {
      DelayedServiceExecutorTopic -> delayedServiceExecutorChannels.getOrPut(entity, newChannel())
      DelayedWorkflowEngineTopic -> delayedWorkflowEngineChannels.getOrPut(entity, newChannel())
      DelayedWorkflowTaskExecutorTopic -> delayedWorkflowTaskExecutorChannels.getOrPut(
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
