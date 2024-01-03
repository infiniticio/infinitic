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
package io.infinitic.common.workflows.engine.events

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.clients.messages.MethodTimedOut
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.WorkflowMethodTimedOutError
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowEventMessage : Message {
  override val messageId: MessageId = MessageId()
  abstract val workflowId: WorkflowId
  abstract val workflowName: WorkflowName
  abstract val workflowTags: Set<WorkflowTag>
  abstract val workflowMeta: WorkflowMeta

  override fun envelope() = WorkflowEventEnvelope.from(this)

}

sealed interface WorkflowMethodEvent {
  val workflowId: WorkflowId
  val workflowName: WorkflowName
  val workflowMethodId: WorkflowMethodId
  val parentWorkflowName: WorkflowName?
  val parentWorkflowId: WorkflowId?
  val parentWorkflowMethodId: WorkflowMethodId?
  val parentClientName: ClientName?
  val waitingClients: Set<ClientName>

  fun isItsOwnParent() = (parentWorkflowId == workflowId && workflowName == parentWorkflowName)
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowStartedEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowCompletedEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowCanceledEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val cancellationReason: WorkflowCancellationReason,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodStartedEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override val parentWorkflowName: WorkflowName?,
  override val parentWorkflowId: WorkflowId?,
  override val parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage(), WorkflowMethodEvent

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodCompletedEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override val parentWorkflowName: WorkflowName?,
  override val parentWorkflowId: WorkflowId?,
  override val parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val returnValue: ReturnValue,
) : WorkflowEventMessage(), WorkflowMethodEvent {
  fun getEventsForClient() = waitingClients.map {
    MethodCompleted(
        recipientName = it,
        workflowId = workflowId,
        workflowMethodId = workflowMethodId,
        methodReturnValue = returnValue,
        emitterName = emitterName,
    )
  }

  fun getEventForParentWorkflow() = parentWorkflowId?.let {
    ChildMethodCompleted(
        childWorkflowReturnValue = WorkflowReturnValue(
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            returnValue = returnValue,
        ),
        workflowId = parentWorkflowId,
        workflowName = parentWorkflowName ?: thisShouldNotHappen(),
        workflowMethodId = parentWorkflowMethodId ?: thisShouldNotHappen(),
        emitterName = emitterName,
    )
  }
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodFailedEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override val parentWorkflowName: WorkflowName?,
  override val parentWorkflowId: WorkflowId?,
  override val parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val workflowMethodName: MethodName,
  val deferredError: DeferredError
) : WorkflowEventMessage(), WorkflowMethodEvent {
  fun getEventsForClient() = waitingClients.map {
    MethodFailed(
        recipientName = it,
        workflowId = workflowId,
        workflowMethodId = workflowMethodId,
        cause = deferredError,
        emitterName = emitterName,
    )
  }

  fun getEventForParentWorkflow() = parentWorkflowId?.let {
    ChildMethodFailed(
        childMethodFailedError = MethodFailedError(
            workflowName = workflowName,
            workflowId = workflowId,
            workflowMethodName = workflowMethodName,
            workflowMethodId = workflowMethodId,
            deferredError = deferredError,
        ),
        workflowId = it,
        workflowName = parentWorkflowName ?: thisShouldNotHappen(),
        workflowMethodId = parentWorkflowMethodId ?: thisShouldNotHappen(),
        emitterName = emitterName,
    )
  }
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodCanceledEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override val parentWorkflowName: WorkflowName?,
  override val parentWorkflowId: WorkflowId?,
  override val parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val cancellationReason: WorkflowCancellationReason,
) : WorkflowEventMessage(), WorkflowMethodEvent {

  fun getEventsForClient() = waitingClients.map {
    MethodCanceled(
        recipientName = it,
        workflowId = workflowId,
        workflowMethodId = workflowMethodId,
        emitterName = emitterName,
    )
  }

  fun getEventForParentWorkflow(): ChildMethodCanceled? =
      if (cancellationReason != WorkflowCancellationReason.CANCELED_BY_PARENT && parentWorkflowId != null) {
        ChildMethodCanceled(
            childMethodCanceledError = MethodCanceledError(
                workflowName = workflowName,
                workflowId = workflowId,
                workflowMethodId = workflowMethodId,
            ),
            workflowId = parentWorkflowId,
            workflowName = parentWorkflowName ?: thisShouldNotHappen(),
            workflowMethodId = parentWorkflowMethodId ?: thisShouldNotHappen(),
            emitterName = emitterName,
        )
      } else null
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodTimedOutEvent(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override val parentWorkflowName: WorkflowName?,
  override val parentWorkflowId: WorkflowId?,
  override val parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val workflowMethodName: MethodName,
) : WorkflowEventMessage(), WorkflowMethodEvent {
  fun getEventsForClient() = waitingClients.map {
    MethodTimedOut(
        recipientName = it,
        workflowId = workflowId,
        workflowMethodId = workflowMethodId,
        emitterName = emitterName,
    )
  }

  fun getEventForParentWorkflow(): ChildMethodTimedOut? =
      if (parentWorkflowId != null) {
        ChildMethodTimedOut(
            childMethodTimedOutError = WorkflowMethodTimedOutError(
                workflowName = workflowName,
                workflowId = workflowId,
                workflowMethodId = workflowMethodId,
                methodName = workflowMethodName,
            ),
            workflowId = parentWorkflowId,
            workflowName = parentWorkflowName ?: thisShouldNotHappen(),
            workflowMethodId = parentWorkflowMethodId ?: thisShouldNotHappen(),
            emitterName = emitterName,
        )
      } else null
}
