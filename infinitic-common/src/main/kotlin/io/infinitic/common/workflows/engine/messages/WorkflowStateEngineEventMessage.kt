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
package io.infinitic.common.workflows.engine.messages

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.Requester
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowStateEngineEventMessage : WorkflowMessageInterface {
  val workflowVersion: WorkflowVersion?
}

fun WorkflowStateEngineEventMessage.type(): WorkflowStateEngineEventMessageType = when (this) {
  is WorkflowCompletedEvent -> WorkflowStateEngineEventMessageType.WORKFLOW_COMPLETED
  is WorkflowCanceledEvent -> WorkflowStateEngineEventMessageType.WORKFLOW_CANCELED
  is MethodCommandedEvent -> WorkflowStateEngineEventMessageType.METHOD_DISPATCHED
  is MethodCompletedEvent -> WorkflowStateEngineEventMessageType.METHOD_COMPLETED
  is MethodFailedEvent -> WorkflowStateEngineEventMessageType.METHOD_FAILED
  is MethodCanceledEvent -> WorkflowStateEngineEventMessageType.METHOD_CANCELED
  is MethodTimedOutEvent -> WorkflowStateEngineEventMessageType.METHOD_TIMED_OUT
  is TaskDispatchedEvent -> WorkflowStateEngineEventMessageType.TASK_DISPATCHED
  is RemoteMethodDispatchedEvent -> WorkflowStateEngineEventMessageType.REMOTE_METHOD_DISPATCHED
  is TimerDispatchedEvent -> WorkflowStateEngineEventMessageType.TIMER_DISPATCHED
  is SignalDispatchedEvent -> WorkflowStateEngineEventMessageType.REMOTE_SIGNAL_DISPATCHED
  is SignalDiscardedEvent -> WorkflowStateEngineEventMessageType.SIGNAL_DISCARDED
  is SignalReceivedEvent -> WorkflowStateEngineEventMessageType.SIGNAL_RECEIVED
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
@AvroName("WorkflowEventMessageType")
enum class WorkflowStateEngineEventMessageType {
  WORKFLOW_COMPLETED,
  WORKFLOW_CANCELED,
  METHOD_DISPATCHED,
  METHOD_COMPLETED,
  METHOD_FAILED,
  METHOD_CANCELED,
  METHOD_TIMED_OUT,
  TASK_DISPATCHED,
  REMOTE_METHOD_DISPATCHED,
  TIMER_DISPATCHED,
  REMOTE_SIGNAL_DISPATCHED,
  SIGNAL_RECEIVED,
  SIGNAL_DISCARDED,
}

interface MethodTerminated : WorkflowMessageInterface {
  val workflowMethodName: MethodName
  val workflowMethodId: WorkflowMethodId
  val awaitingRequesters: Set<Requester>

  fun getEventForAwaitingClients(emitterName: EmitterName): List<ClientMessage>
  fun getEventForAwaitingWorkflows(
    emitterName: EmitterName,
    emittedAt: MillisInstant
  ): List<WorkflowStateEngineMessage>
}
