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
package io.infinitic.common.workflows.data.steps

import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredCanceledError
import io.infinitic.common.tasks.executors.errors.DeferredFailedError
import io.infinitic.common.tasks.executors.errors.DeferredTimedOutError
import io.infinitic.common.tasks.executors.errors.DeferredUnknownError
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.reflect.Method

@Serializable
sealed class StepStatus {

  @Serializable
  @SerialName("StepStatus.Waiting")
  object Waiting : StepStatus() {
    override fun equals(other: Any?) = javaClass == other?.javaClass
  }

  @Serializable
  @SerialName("StepStatus.Unknown")
  data class Unknown(
    @SerialName("unknownDeferredError")
    val deferredUnknownError: DeferredUnknownError,
    val unknowingWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  @Serializable
  @SerialName("StepStatus.Canceled")
  data class Canceled(
    @SerialName("canceledDeferredError")
    val deferredCanceledError: DeferredCanceledError,
    val cancellationWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  /**
   * CurrentlyTimedOut is a transient status given when the timeout of a task triggers the failure of
   * a step
   * - if next workflowTask related to this branch run correctly (error caught in workflow code),
   *   status will eventually be TimedOut
   * - if not, task can be retried and status can transition to Completed
   */
  @Serializable
  @SerialName("StepStatus.CurrentlyTimedOut")
  data class CurrentlyTimedOut(
    val deferredTimedOutError: DeferredTimedOutError,
    val timeoutWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  @Serializable
  @SerialName("StepStatus.TimedOut")
  data class TimedOut(
    val deferredTimedOutError: DeferredTimedOutError,
    val timeoutWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  /**
   * CurrentlyFailed is a transient status given when the failure of a task triggers the failure of
   * a step
   * - if next workflowTask related to this branch run correctly (error caught in workflow code),
   *   status will eventually be Failed
   * - if not, task can be retried and status can transition to Completed
   */
  @Serializable
  @SerialName("StepStatus.CurrentlyFailed")
  data class CurrentlyFailed(
    @SerialName("failedDeferredError")
    val deferredFailedError: DeferredFailedError,
    val failureWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  @Serializable
  @SerialName("StepStatus.Failed")
  data class Failed(
    @SerialName("failedDeferredError")
    val deferredFailedError: DeferredFailedError,
    val failureWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus()

  @Serializable
  @SerialName("StepStatus.Completed")
  data class Completed(
    private val returnValue: MethodReturnValue,
    val completionWorkflowTaskIndex: WorkflowTaskIndex
  ) : StepStatus() {
    @Transient
    var method: Method? = null

    val value: Any? by lazy { returnValue.value(method) }
  }
}
