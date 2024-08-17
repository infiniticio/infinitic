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
import io.infinitic.common.requester.Requester
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowCmdMessage : WorkflowStateEngineMessage {
  val requester: Requester?
}

fun WorkflowCmdMessage.type(): WorkflowCmdMessageType = when (this) {
  is CancelWorkflow -> WorkflowCmdMessageType.CANCEL_WORKFLOW
  is CompleteTimers -> WorkflowCmdMessageType.COMPLETE_TIMERS
  is CompleteWorkflow -> WorkflowCmdMessageType.COMPLETE_WORKFLOW
  is DispatchMethod -> WorkflowCmdMessageType.DISPATCH_METHOD
  is DispatchWorkflow -> WorkflowCmdMessageType.DISPATCH_WORKFLOW
  is RetryTasks -> WorkflowCmdMessageType.RETRY_TASKS
  is RetryWorkflowTask -> WorkflowCmdMessageType.RETRY_WORKFLOW_TASK
  is SendSignal -> WorkflowCmdMessageType.SEND_SIGNAL
  is WaitWorkflow -> WorkflowCmdMessageType.WAIT_WORKFLOW
}

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
enum class WorkflowCmdMessageType {
  WAIT_WORKFLOW,
  CANCEL_WORKFLOW,
  RETRY_WORKFLOW_TASK,
  RETRY_TASKS,
  COMPLETE_TIMERS,
  COMPLETE_WORKFLOW,
  SEND_SIGNAL,
  DISPATCH_WORKFLOW,
  DISPATCH_METHOD,
}

