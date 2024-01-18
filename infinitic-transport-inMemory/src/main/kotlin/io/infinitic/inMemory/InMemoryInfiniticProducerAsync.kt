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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.topics.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.topics.NamingTopic
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
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class InMemoryInfiniticProducerAsync(private val channels: InMemoryChannels) :
  InfiniticProducerAsync {

  private val logger = KotlinLogging.logger {}

  override var producerName = DEFAULT_NAME

  @Suppress("UNCHECKED_CAST")
  private fun <S : Message> Topic<S>.channelForMessage(message: S): Channel<Any> = when (this) {
    WorkflowTagTopic -> channels.forWorkflowTag(WorkflowName(message.entity()))
    WorkflowCmdTopic -> channels.forWorkflowCmd(WorkflowName(message.entity()))
    WorkflowEngineTopic -> channels.forWorkflowEngine(WorkflowName(message.entity()))
    DelayedWorkflowEngineTopic -> channels.forDelayedWorkflowEngine(WorkflowName(message.entity()))
    WorkflowEventsTopic -> channels.forWorkflowEvent(WorkflowName(message.entity()))
    WorkflowTaskExecutorTopic -> channels.forWorkflowTaskExecutor(WorkflowName(message.entity()))
    DelayedWorkflowTaskExecutorTopic -> channels.forDelayedWorkflowTaskExecutor(
        WorkflowName(message.entity()),
    )

    WorkflowTaskEventsTopic -> channels.forWorkflowTaskEvents(WorkflowName(message.entity()))
    ServiceTagTopic -> channels.forTaskTag(ServiceName(message.entity()))
    ServiceExecutorTopic -> channels.forTaskExecutor(ServiceName(message.entity()))
    DelayedServiceExecutorTopic -> channels.forDelayedTaskExecutor(ServiceName(message.entity()))
    ServiceEventsTopic -> channels.forTaskEvents(ServiceName(message.entity()))
    ClientTopic -> channels.forClient(ClientName(message.entity()))
    NamingTopic -> thisShouldNotHappen()
  } as Channel<Any>

  override fun <T : Message> internalSendToAsync(
    message: T,
    topic: Topic<T>,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val msg: Any = when (after > 0) {
      true -> DelayedMessage(message, after)
      false -> message
    }
    val channel = topic.channelForMessage(message)
    logger.debug { "Channel ${channel.id}: sending $msg" }
    val future = with(channels) { channel.sendAsync(msg) }
    logger.trace { "Channel ${channel.id}: sent" }

    return future
  }

  companion object {
    private const val DEFAULT_NAME = "inMemory"
  }
}


