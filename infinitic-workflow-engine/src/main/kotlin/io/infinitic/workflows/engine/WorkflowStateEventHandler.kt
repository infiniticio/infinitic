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
package io.infinitic.workflows.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.SignalDiscardedEvent
import io.infinitic.common.workflows.engine.messages.SignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.SignalReceivedEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowStateEventHandler(val producer: InfiniticProducer) {

  private suspend fun getEmitterName() = EmitterName(producer.getName())

  suspend fun handle(msg: WorkflowStateEventMessage, publishTime: MillisInstant) {
    when (msg) {
      is WorkflowCanceledEvent -> Unit
      is WorkflowCompletedEvent -> Unit
      is MethodCommandedEvent -> Unit
      is SignalDiscardedEvent -> Unit
      is SignalReceivedEvent -> Unit
      is MethodCanceledEvent -> sendWorkflowMethodCanceled(msg, publishTime)
      is MethodCompletedEvent -> sendWorkflowMethodCompleted(msg, publishTime)
      is MethodFailedEvent -> sendWorkflowMethodFailed(msg, publishTime)
      is MethodTimedOutEvent -> sendWorkflowMethodTimedOut(msg, publishTime)
      is TaskDispatchedEvent -> Unit
      is RemoteMethodDispatchedEvent -> Unit
      is TimerDispatchedEvent -> Unit
      is SignalDispatchedEvent -> Unit
    }
  }

  private suspend fun sendWorkflowMethodCanceled(
    msg: MethodCanceledEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(getEmitterName()).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(getEmitterName(), publishTime)
        .filter { it.workflowId != msg.workflowId }
        .forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowStateEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodCompleted(
    msg: MethodCompletedEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(getEmitterName()).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(getEmitterName(), publishTime)
        .filter { it.workflowId != msg.workflowId }
        .forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowStateEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodFailed(
    msg: MethodFailedEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(getEmitterName()).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(getEmitterName(), publishTime)
        .filter { it.workflowId != msg.workflowId }
        .forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowStateEngineTopic) } }
        }
  }

  private suspend fun sendWorkflowMethodTimedOut(
    msg: MethodTimedOutEvent,
    publishTime: MillisInstant
  ) = coroutineScope {
    // tell awaiting clients
    msg.getEventForAwaitingClients(getEmitterName()).forEach { event ->
      launch { with(producer) { event.sendTo(ClientTopic) } }
    }

    // tell awaiting workflow (except itself)
    msg.getEventForAwaitingWorkflows(getEmitterName(), publishTime)
        .filter { it.workflowId != msg.workflowId }
        .forEach { event ->
          launch { with(producer) { event.sendTo(WorkflowStateEngineTopic) } }
        }
  }

  companion object {
    val logger = KotlinLogging.logger { }
  }
}
