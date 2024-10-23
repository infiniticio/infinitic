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
package io.infinitic.events.listeners

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.interfaces.InfiniticConsumer
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.consumers.Result
import io.infinitic.common.transport.consumers.startConsuming
import io.infinitic.common.transport.create
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

context(CoroutineScope, KLogger)
internal fun InfiniticConsumer.listenToWorkflowStateTopics(
  workflowName: WorkflowName,
  subscriptionName: String?,
  outChannel: Channel<Result<TransportMessage<Message>, TransportMessage<Message>>>,
): Job = launch {

  // Send messages from WorkflowStateCmdTopic to inChannel
  val workflowStateCmdSubscription = SubscriptionType.EVENT_LISTENER.create(
      WorkflowStateCmdTopic,
      subscriptionName,
  )
  buildConsumer(workflowStateCmdSubscription, workflowName.toString())
      .startConsuming(outChannel)

  // Send messages from WorkflowStateEngineTopic to inChannel
  val workflowStateEngineSubscription = SubscriptionType.EVENT_LISTENER.create(
      WorkflowStateEngineTopic,
      subscriptionName,
  )
  buildConsumer(workflowStateEngineSubscription, workflowName.toString())
      .startConsuming(outChannel)

  // Send messages from WorkflowStateEventTopic to inChannel
  val workflowStateEventSubscription = SubscriptionType.EVENT_LISTENER.create(
      WorkflowStateEventTopic,
      subscriptionName,
  )
  buildConsumer(workflowStateEventSubscription, workflowName.toString())
      .startConsuming(outChannel)
}
