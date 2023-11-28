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
import io.infinitic.exceptions.DeferredCanceledException
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.DeferredFailedException
import io.infinitic.exceptions.DeferredTimedOutException
import io.infinitic.exceptions.DeferredUnknownException
import io.infinitic.exceptions.TaskCanceledException
import io.infinitic.exceptions.TaskFailedException
import io.infinitic.exceptions.TaskTimedOutException
import io.infinitic.exceptions.TaskUnknownException
import io.infinitic.exceptions.WorkflowCanceledException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowTaskFailedException
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.exceptions.WorkflowUnknownException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
sealed class DeferredError {
  companion object {
    fun from(exception: DeferredException) =
        when (exception) {
          is DeferredFailedException -> DeferredFailedError.from(exception)
          is DeferredCanceledException -> DeferredCanceledError.from(exception)
          is DeferredTimedOutException -> DeferredTimedOutError.from(exception)
          is DeferredUnknownException -> DeferredUnknownError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredFailedError : DeferredError() {
  companion object {
    fun from(exception: DeferredFailedException) =
        when (exception) {
          is TaskFailedException -> TaskFailedError.from(exception)
          is WorkflowFailedException -> WorkflowFailedError.from(exception)
          is WorkflowTaskFailedException -> WorkflowTaskFailedError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredCanceledError : DeferredError() {
  companion object {
    fun from(exception: DeferredCanceledException) =
        when (exception) {
          is TaskCanceledException -> TaskCanceledError.from(exception)
          is WorkflowCanceledException -> WorkflowCanceledError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredTimedOutError : DeferredError() {
  companion object {
    fun from(exception: DeferredTimedOutException) =
        when (exception) {
          is TaskTimedOutException -> TaskTimedOutError.from(exception)
          is WorkflowTimedOutException -> WorkflowTimedOutError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredUnknownError : DeferredError() {
  companion object {
    fun from(exception: DeferredUnknownException) =
        when (exception) {
          is TaskUnknownException -> TaskUnknownError.from(exception)
          is WorkflowUnknownException -> WorkflowUnknownError.from(exception)
        }
  }
}

/** Error occurring when waiting for an unknown task */
@Serializable
@SerialName("UnknownTaskError")
data class TaskUnknownError(
  /** Name of the unknown task */
  @SerialName("taskName")
  val serviceName: ServiceName,

  /** Id of the unknown task */
  val taskId: TaskId
) : DeferredUnknownError() {
  companion object {
    fun from(exception: TaskUnknownException) =
        TaskUnknownError(
            serviceName = ServiceName(exception.serviceName), taskId = TaskId(exception.taskId),
        )
  }
}

/** Error occurring when waiting for an unknown workflow */
@Serializable
@SerialName("UnknownWorkflowError")
data class WorkflowUnknownError(
  /** Name of the unknown workflow */
  val workflowName: WorkflowName,

  /** Id of the unknown workflow */
  val workflowId: WorkflowId,

  /** Id of the unknown workflow' method run */
  val methodRunId: MethodRunId?
) : DeferredUnknownError() {
  companion object {
    fun from(exception: WorkflowUnknownException) =
        WorkflowUnknownError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
        )
  }
}

/** Error occurring when waiting a timed-out task */
@Serializable
@SerialName("TimedOutTaskError")
data class TaskTimedOutError(
  /** Name of the timed-out task */
  @SerialName("taskName")
  val serviceName: ServiceName,

  /** Id of the timed-out task */
  val taskId: TaskId,

  /** Method of the timed-out task */
  val methodName: MethodName
) : DeferredTimedOutError() {
  companion object {
    fun from(exception: TaskTimedOutException) =
        TaskTimedOutError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName),
        )
  }
}

/** Error occurring when waiting a timed-out child workflow */
@Serializable
@SerialName("TimedOutWorkflowError")
data class WorkflowTimedOutError(
  /** Name of timed-out child workflow */
  val workflowName: WorkflowName,

  /** Id of timed-out child workflow */
  val workflowId: WorkflowId,

  /** Method of timed-out child workflow */
  val methodName: MethodName,

  /** Id of the methodRun */
  val methodRunId: MethodRunId?
) : DeferredTimedOutError() {
  companion object {
    fun from(exception: WorkflowTimedOutException) =
        WorkflowTimedOutError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
        )
  }
}

/** Error occurring when waiting a canceled task */
@Serializable
@SerialName("CanceledTaskError")
data class TaskCanceledError(
  /** Name of canceled task */
  @SerialName("taskName") val serviceName: ServiceName,

  /** Id of canceled task */
  val taskId: TaskId,

  /** Method of canceled task */
  val methodName: MethodName
) : DeferredCanceledError() {
  companion object {
    fun from(exception: TaskCanceledException) =
        TaskCanceledError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName),
        )
  }
}

/** Error occurring when waiting a failed child workflow */
@Serializable
@SerialName("CanceledWorkflowError")
data class WorkflowCanceledError(
  /** Name of canceled child workflow */
  val workflowName: WorkflowName,

  /** Id of canceled child workflow */
  val workflowId: WorkflowId,

  /** Id of the methodRun */
  val methodRunId: MethodRunId?
) : DeferredCanceledError() {
  companion object {
    fun from(exception: WorkflowCanceledException) =
        WorkflowCanceledError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
        )
  }
}

/** Error occurring when waiting a failed task */
@Serializable
@SerialName("FailedTaskError")
data class TaskFailedError(
  /** Name of failed task */
  @SerialName("taskName") val serviceName: ServiceName,

  /** Id of failed task */
  val taskId: TaskId,

  /** Method of failed task */
  val methodName: MethodName,

  /** cause of the error */
  val cause: ExecutionError
) : DeferredFailedError() {
  companion object {
    fun from(exception: TaskFailedException) =
        TaskFailedError(
            serviceName = ServiceName(exception.serviceName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName),
            cause = ExecutionError.from(exception.workerException),
        )
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowError")
data class WorkflowFailedError(
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
) : DeferredFailedError() {
  companion object {
    fun from(exception: WorkflowFailedException): WorkflowFailedError =
        WorkflowFailedError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
            deferredError = from(exception.deferredException),
        )
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowTaskError")
data class WorkflowTaskFailedError(
  /** Name of failed workflow */
  val workflowName: WorkflowName,

  /** Id of failed workflow */
  val workflowId: WorkflowId,

  /** Id of failed workflow task */
  val workflowTaskId: TaskId,

  /** cause of the error */
  val cause: ExecutionError
) : DeferredFailedError() {
  companion object {
    fun from(exception: WorkflowTaskFailedException): WorkflowTaskFailedError =
        WorkflowTaskFailedError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            workflowTaskId = TaskId(exception.workflowTaskId),
            cause = ExecutionError.from(exception.workerException),
        )
  }
}
