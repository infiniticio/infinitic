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

package io.infinitic.events.data.workflows

import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.events.InfiniticWorkflowEventType

fun WorkflowCmdMessage.workflowType(): InfiniticWorkflowEventType = when (this) {
  is CancelWorkflow -> TODO()
  is CompleteTimers -> TODO()
  is CompleteWorkflow -> TODO()
  is DispatchMethodWorkflow -> TODO()
  is DispatchNewWorkflow -> TODO()
  is RetryTasks -> TODO()
  is RetryWorkflowTask -> TODO()
  is SendSignal -> TODO()
  is WaitWorkflow -> TODO()
}

//
//@Serializable
//data class WorkflowMethodTaskDispatchedData(
//  override val workflowMethodName: String,
//  override val workflowMethodId: String,
//  override val serviceName: String,
//  override val taskName: String,
//  override val taskId: String,
//  val taskArgs: List<JsonElement>,
//  val meta: Map<String, ByteArray>,
//  val tags: Set<String>,
//  val workerName: String,
//  override val infiniticVersion: String
//) : WorkflowMethodTaskEventData
//
fun WorkflowCmdMessage.toWorkflowData(): WorkflowEventData? = when (this) {
  is CancelWorkflow -> TODO()
  is CompleteTimers -> TODO()
  is CompleteWorkflow -> TODO()
  is DispatchMethodWorkflow -> TODO()
  is DispatchNewWorkflow -> TODO()
  is RetryTasks -> TODO()
  is RetryWorkflowTask -> TODO()
  is SendSignal -> TODO()
  is WaitWorkflow -> TODO()
}
