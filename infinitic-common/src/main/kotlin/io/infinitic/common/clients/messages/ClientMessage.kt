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
package io.infinitic.common.clients.messages

import com.github.avrokotlin.avro4k.AvroName
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage : Message {
  override val messageId: MessageId = MessageId()
  abstract override val emitterName: EmitterName
  abstract val recipientName: ClientName
  
  override fun key() = null

  override fun entity() = recipientName.toString()
}

@Serializable
data class TaskCompleted(
  override val emitterName: EmitterName,
  override val recipientName: ClientName,
  override val taskId: TaskId,
  val taskReturnValue: ReturnValue,
  val taskMeta: TaskMeta
) : ClientMessage(), TaskMessage

@Serializable
data class TaskFailed(
  override val recipientName: ClientName,
  override val taskId: TaskId,
  val cause: ExecutionError,
  override val emitterName: EmitterName
) : ClientMessage(), TaskMessage

@Serializable
data class TaskCanceled(
  override val recipientName: ClientName,
  override val taskId: TaskId,
  override val emitterName: EmitterName
) : ClientMessage(), TaskMessage

@Serializable
data class TaskIdsByTag(
  override val recipientName: ClientName,
  @SerialName("taskName") val serviceName: ServiceName,
  val taskTag: TaskTag,
  val taskIds: Set<TaskId>,
  override val emitterName: EmitterName
) : ClientMessage()

@Serializable
data class MethodCompleted(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  val methodReturnValue: ReturnValue,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
data class MethodFailed(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  val cause: DeferredError,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
data class MethodCanceled(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
data class MethodTimedOut(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
@AvroName("MethodRunUnknown")
data class MethodUnknown(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
data class MethodAlreadyCompleted(
  override val recipientName: ClientName,
  override val workflowId: WorkflowId,
  @SerialName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName
) : ClientMessage(), MethodMessage

@Serializable
data class WorkflowIdsByTag(
  override val recipientName: ClientName,
  val workflowName: WorkflowName,
  val workflowTag: WorkflowTag,
  val workflowIds: Set<WorkflowId>,
  override val emitterName: EmitterName
) : ClientMessage()
