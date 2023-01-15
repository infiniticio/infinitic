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
package io.infinitic.common.tasks.executors.errors

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.CanceledDeferredException
import io.infinitic.exceptions.CanceledTaskException
import io.infinitic.exceptions.CanceledWorkflowException
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.FailedDeferredException
import io.infinitic.exceptions.FailedTaskException
import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.FailedWorkflowTaskException
import io.infinitic.exceptions.TimedOutDeferredException
import io.infinitic.exceptions.TimedOutTaskException
import io.infinitic.exceptions.TimedOutWorkflowException
import io.infinitic.exceptions.UnknownDeferredException
import io.infinitic.exceptions.UnknownTaskException
import io.infinitic.exceptions.UnknownWorkflowException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
sealed class DeferredError {
  companion object {
    fun from(exception: DeferredException) =
        when (exception) {
          is FailedDeferredException -> FailedDeferredError.from(exception)
          is CanceledDeferredException -> CanceledDeferredError.from(exception)
          is TimedOutDeferredException -> TimedOutDeferredError.from(exception)
          is UnknownDeferredException -> UnknownDeferredError.from(exception)
        }
  }
}

@Serializable
sealed class FailedDeferredError : DeferredError() {
  companion object {
    fun from(exception: FailedDeferredException) =
        when (exception) {
          is FailedTaskException -> FailedTaskError.from(exception)
          is FailedWorkflowException -> FailedWorkflowError.from(exception)
          is FailedWorkflowTaskException -> FailedWorkflowTaskError.from(exception)
        }
  }
}

@Serializable
sealed class CanceledDeferredError : DeferredError() {
  companion object {
    fun from(exception: CanceledDeferredException) =
        when (exception) {
          is CanceledTaskException -> CanceledTaskError.from(exception)
          is CanceledWorkflowException -> CanceledWorkflowError.from(exception)
        }
  }
}

@Serializable
sealed class TimedOutDeferredError : DeferredError() {
  companion object {
    fun from(exception: TimedOutDeferredException) =
        when (exception) {
          is TimedOutTaskException -> TimedOutTaskError.from(exception)
          is TimedOutWorkflowException -> TimedOutWorkflowError.from(exception)
        }
  }
}

@Serializable
sealed class UnknownDeferredError : DeferredError() {
  companion object {
    fun from(exception: UnknownDeferredException) =
        when (exception) {
          is UnknownTaskException -> UnknownTaskError.from(exception)
          is UnknownWorkflowException -> UnknownWorkflowError.from(exception)
        }
  }
}

/** Error occurring when waiting for an unknown task */
@Serializable
@SerialName("UnknownTaskError")
data class UnknownTaskError(
    /** Name of the unknown task */
    @SerialName("taskName") val serviceName: ServiceName,

    /** Id of the unknown task */
    val taskId: TaskId
) : UnknownDeferredError() {
  companion object {
    fun from(exception: UnknownTaskException) =
        UnknownTaskError(
            serviceName = ServiceName(exception.serviceName), taskId = TaskId(exception.taskId))
  }
}

/** Error occurring when waiting for an unknown workflow */
@Serializable
@SerialName("UnknownWorkflowError")
data class UnknownWorkflowError(
    /** Name of the unknown workflow */
    val workflowName: WorkflowName,

    /** Id of the unknown workflow */
    val workflowId: WorkflowId,

    /** Id of the unknown workflow' method run */
    val methodRunId: MethodRunId?
) : UnknownDeferredError() {
  companion object {
    fun from(exception: UnknownWorkflowException) =
        UnknownWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) })
  }
}

/** Error occurring when waiting a timed-out task */
@Serializable
@SerialName("TimedOutTaskError")
data class TimedOutTaskError(
    /** Name of the timed-out task */
    @SerialName("taskName") val serviceName: ServiceName,

    /** Id of the timed-out task */
    val taskId: TaskId,

    /** Method of the timed-out task */
    val methodName: MethodName
) : TimedOutDeferredError() {
  companion object {
    fun from(exception: TimedOutTaskException) =
        TimedOutTaskError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName))
  }
}

/** Error occurring when waiting a timed-out child workflow */
@Serializable
@SerialName("TimedOutWorkflowError")
data class TimedOutWorkflowError(
    /** Name of timed-out child workflow */
    val workflowName: WorkflowName,

    /** Id of timed-out child workflow */
    val workflowId: WorkflowId,

    /** Method of timed-out child workflow */
    val methodName: MethodName,

    /** Id of the methodRun */
    val methodRunId: MethodRunId?
) : TimedOutDeferredError() {
  companion object {
    fun from(exception: TimedOutWorkflowException) =
        TimedOutWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) })
  }
}

/** Error occurring when waiting a canceled task */
@Serializable
@SerialName("CanceledTaskError")
data class CanceledTaskError(
    /** Name of canceled task */
    @SerialName("taskName") val serviceName: ServiceName,

    /** Id of canceled task */
    val taskId: TaskId,

    /** Method of canceled task */
    val methodName: MethodName
) : CanceledDeferredError() {
  companion object {
    fun from(exception: CanceledTaskException) =
        CanceledTaskError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName))
  }
}

/** Error occurring when waiting a failed child workflow */
@Serializable
@SerialName("CanceledWorkflowError")
data class CanceledWorkflowError(
    /** Name of canceled child workflow */
    val workflowName: WorkflowName,

    /** Id of canceled child workflow */
    val workflowId: WorkflowId,

    /** Id of the methodRun */
    val methodRunId: MethodRunId?
) : CanceledDeferredError() {
  companion object {
    fun from(exception: CanceledWorkflowException) =
        CanceledWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) })
  }
}

/** Error occurring when waiting a failed task */
@Serializable
@SerialName("FailedTaskError")
data class FailedTaskError(
    /** Name of failed task */
    @SerialName("taskName") val serviceName: ServiceName,

    /** Id of failed task */
    val taskId: TaskId,

    /** Method of failed task */
    val methodName: MethodName,

    /** cause of the error */
    val cause: ExecutionError
) : FailedDeferredError() {
  companion object {
    fun from(exception: FailedTaskException) =
        FailedTaskError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName),
            cause = ExecutionError.from(exception.workerException))
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowError")
data class FailedWorkflowError(
    /** Name of failed child workflow */
    val workflowName: WorkflowName,

    /** Method of failed child workflow */
    val methodName: MethodName,

    /** Id of failed child workflow */
    val workflowId: WorkflowId,

    /** Id of failed method run */
    val methodRunId: MethodRunId?,

    /** error */
    val deferredError: DeferredError
) : FailedDeferredError() {
  companion object {
    fun from(exception: FailedWorkflowException): FailedWorkflowError =
        FailedWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
            deferredError = from(exception.deferredException))
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowTaskError")
data class FailedWorkflowTaskError(
    /** Name of failed workflow */
    val workflowName: WorkflowName,

    /** Id of failed workflow */
    val workflowId: WorkflowId,

    /** Id of failed workflow task */
    val workflowTaskId: TaskId,

    /** cause of the error */
    val cause: ExecutionError
) : FailedDeferredError() {
  companion object {
    fun from(exception: FailedWorkflowTaskException): FailedWorkflowTaskError =
        FailedWorkflowTaskError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            workflowTaskId = TaskId(exception.workflowTaskId),
            cause = ExecutionError.from(exception.workerException))
  }
}
