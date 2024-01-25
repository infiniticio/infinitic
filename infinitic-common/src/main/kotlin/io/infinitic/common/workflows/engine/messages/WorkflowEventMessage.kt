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

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowEventMessage : WorkflowMessageInterface {
  val workflowTags: Set<WorkflowTag>
  val workflowMeta: WorkflowMeta
}

fun WorkflowEventMessage.type(): WorkflowEventMessageType = when (this) {
  is WorkflowStartedEvent -> WorkflowEventMessageType.WORKFLOW_STARTED
  is WorkflowCompletedEvent -> WorkflowEventMessageType.WORKFLOW_COMPLETED
  is WorkflowCanceledEvent -> WorkflowEventMessageType.WORKFLOW_CANCELED
  is WorkflowMethodStartedEvent -> WorkflowEventMessageType.WORKFLOW_METHOD_STARTED
  is WorkflowMethodCompletedEvent -> WorkflowEventMessageType.WORKFLOW_METHOD_COMPLETED
  is WorkflowMethodFailedEvent -> WorkflowEventMessageType.WORKFLOW_METHOD_FAILED
  is WorkflowMethodCanceledEvent -> WorkflowEventMessageType.WORKFLOW_METHOD_CANCELED
  is WorkflowMethodTimedOutEvent -> WorkflowEventMessageType.WORKFLOW_METHOD_TIMED_OUT
}

@Serializable
@AvroNamespace("io.infinitic.workflows.events")
enum class WorkflowEventMessageType {
  WORKFLOW_STARTED,
  WORKFLOW_COMPLETED,
  WORKFLOW_CANCELED,
  WORKFLOW_METHOD_STARTED,
  WORKFLOW_METHOD_COMPLETED,
  WORKFLOW_METHOD_FAILED,
  WORKFLOW_METHOD_CANCELED,
  WORKFLOW_METHOD_TIMED_OUT,
  //TASK_DISPATCHED,
  //CHILD_WORKFLOW_DISPATCHED,
}

sealed interface WorkflowMethodMessage {
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
