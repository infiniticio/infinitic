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
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

interface WorkflowInternalEvent

interface MethodEvent : WorkflowInternalEvent {
  val workflowMethodId: WorkflowMethodId
}

interface TaskEvent : MethodEvent {
  fun taskId(): TaskId

  fun serviceName(): ServiceName
}

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
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowStarted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowCompleted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowCanceled(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodStarted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override var parentWorkflowName: WorkflowName?,
  override var parentWorkflowId: WorkflowId?,
  override var parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage(), WorkflowMethodEvent

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodCompleted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override var parentWorkflowName: WorkflowName?,
  override var parentWorkflowId: WorkflowId?,
  override var parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val returnValue: ReturnValue,
) : WorkflowEventMessage(), WorkflowMethodEvent

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodFailed(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override var parentWorkflowName: WorkflowName?,
  override var parentWorkflowId: WorkflowId?,
  override var parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
  val deferredError: DeferredError
) : WorkflowEventMessage(), WorkflowMethodEvent

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodCanceled(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override var parentWorkflowName: WorkflowName?,
  override var parentWorkflowId: WorkflowId?,
  override var parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage(), WorkflowMethodEvent

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
data class WorkflowMethodTimedOut(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowMethodId: WorkflowMethodId,
  override var parentWorkflowName: WorkflowName?,
  override var parentWorkflowId: WorkflowId?,
  override var parentWorkflowMethodId: WorkflowMethodId?,
  override val parentClientName: ClientName?,
  override val waitingClients: Set<ClientName>,
  override val emitterName: EmitterName,
  override val workflowTags: Set<WorkflowTag>,
  override val workflowMeta: WorkflowMeta,
) : WorkflowEventMessage(), WorkflowMethodEvent
