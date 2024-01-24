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

package io.infinitic.events.data

import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import kotlinx.serialization.Serializable

@Serializable
sealed interface DeferredErrorData

@Serializable
data class TaskErrorData(
  val taskId: String,
  val serviceName: String,
  val taskName: String,
  val error: ErrorData
) : DeferredErrorData

@Serializable
data class WorkflowErrorData(
  val workflowId: String,
  val workflowName: String,
  val workflowMethodId: String,
  val error: DeferredErrorData
) : DeferredErrorData

@Serializable
data class WorkflowTaskErrorData(
  val workflowId: String,
  val workflowName: String,
  val workflowTaskId: String,
  val error: ErrorData
) : DeferredErrorData

fun DeferredError.toDeferredErrorData(): DeferredErrorData = when (this) {
  is WorkflowTaskFailedError -> WorkflowTaskErrorData(
      workflowId = workflowId.toString(),
      workflowName = workflowName.toString(),
      workflowTaskId = workflowTaskId.toString(),
      error = cause.toErrorData(),
  )

  is TaskFailedError -> TODO()
  is TaskCanceledError -> TODO()
  is TaskTimedOutError -> TODO()
  is TaskUnknownError -> TODO()
  is MethodFailedError -> TODO()
  is MethodCanceledError -> TODO()
  is MethodTimedOutError -> TODO()
  is MethodUnknownError -> TODO()
}
