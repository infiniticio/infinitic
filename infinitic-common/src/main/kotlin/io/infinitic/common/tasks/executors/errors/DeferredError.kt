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

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
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

/**
 * DeferredError are internal representation of [DeferredException] used to serialize and transport them
 */

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
sealed class DeferredError {
  companion object {
    fun from(e: DeferredException) = when (e) {
      is DeferredFailedException -> DeferredFailedError.from(e)
      is DeferredCanceledException -> DeferredCanceledError.from(e)
      is DeferredTimedOutException -> DeferredTimedOutError.from(e)
      is DeferredUnknownException -> DeferredUnknownError.from(e)
    }
  }
}

@Serializable
sealed class DeferredFailedError : DeferredError() {
  companion object {
    fun from(e: DeferredFailedException) = when (e) {
      is TaskFailedException -> TaskFailedError.from(e)
      is WorkflowFailedException -> MethodFailedError.from(e)
      is WorkflowTaskFailedException -> WorkflowTaskFailedError.from(e)
    }
  }
}

@Serializable
sealed class DeferredCanceledError : DeferredError() {
  companion object {
    fun from(e: DeferredCanceledException) = when (e) {
      is TaskCanceledException -> TaskCanceledError.from(e)
      is WorkflowCanceledException -> MethodCanceledError.from(e)
    }
  }
}

@Serializable
sealed class DeferredTimedOutError : DeferredError() {
  companion object {
    fun from(e: DeferredTimedOutException) = when (e) {
      is TaskTimedOutException -> TaskTimedOutError.from(e)
      is WorkflowTimedOutException -> MethodTimedOutError.from(e)
    }
  }
}

@Serializable
sealed class DeferredUnknownError : DeferredError() {
  companion object {
    fun from(e: DeferredUnknownException) = when (e) {
      is TaskUnknownException -> TaskUnknownError.from(e)
      is WorkflowUnknownException -> MethodUnknownError.from(e)
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
    fun from(e: TaskUnknownException) = TaskUnknownError(
        serviceName = ServiceName(e.serviceName), taskId = TaskId(e.taskId),
    )
  }
}

/** Error occurring when waiting for an unknown workflow */
@Serializable
@SerialName("UnknownWorkflowError")
data class MethodUnknownError(
  /** Name of the unknown workflow */
  val workflowName: WorkflowName,

  /** Id of the unknown workflow */
  val workflowId: WorkflowId,

  /** Id of the unknown workflowMethod */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?
) : DeferredUnknownError() {
  companion object {
    fun from(e: WorkflowUnknownException) = MethodUnknownError(
        workflowName = WorkflowName(e.workflowName),
        workflowId = WorkflowId(e.workflowId),
        workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
    )
  }
}

/** Error occurring when waiting a timed-out task */
@Serializable
@SerialName("TimedOutTaskError")
data class TaskTimedOutError(
  /** timeout duration*/
  val timeout: MillisDuration,

  /** Name of the timed-out task */
  @SerialName("taskName")
  val serviceName: ServiceName,

  /** Id of the timed-out task */
  val taskId: TaskId,

  /** Method of the timed-out task */
  @SerialName("methodName")
  val taskName: MethodName
) : DeferredTimedOutError() {
  companion object {
    fun from(e: TaskTimedOutException) = TaskTimedOutError(
        timeout = MillisDuration(e.timeoutMillis),
        serviceName = ServiceName(e.serviceName),
        taskId = TaskId(e.taskId),
        taskName = MethodName(e.methodName),
    )
  }
}

/** Error occurring when waiting a timed-out child workflow */
@Serializable
@SerialName("TimedOutWorkflowError")
data class MethodTimedOutError(
  val timeout: MillisDuration,

  /** Name of timed-out child workflow */
  val workflowName: WorkflowName,

  /** Id of timed-out child workflow */
  val workflowId: WorkflowId,

  /** Method of timed-out child workflow */
  @SerialName("methodName")
  val taskName: MethodName,

  /** Id of the methodRun */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?
) : DeferredTimedOutError() {
  companion object {
    fun from(e: WorkflowTimedOutException) = MethodTimedOutError(
        timeout = MillisDuration(e.timeoutMillis),
        workflowName = WorkflowName(e.workflowName),
        workflowId = WorkflowId(e.workflowId),
        taskName = MethodName(e.methodName),
        workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
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
  @SerialName("methodName")
  val taskName: MethodName
) : DeferredCanceledError() {
  companion object {
    fun from(e: TaskCanceledException) = TaskCanceledError(
        serviceName = ServiceName(e.serviceName),
        taskId = TaskId(e.taskId),
        taskName = MethodName(e.methodName),
    )
  }
}

/** Error occurring when waiting a failed child workflow */
@Serializable
@SerialName("CanceledWorkflowError")
data class MethodCanceledError(

  /** Name of canceled child workflow */
  val workflowName: WorkflowName,

  /** Id of canceled child workflow */
  val workflowId: WorkflowId,

  /** Id of the methodRun */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?
) : DeferredCanceledError() {
  companion object {
    fun from(e: WorkflowCanceledException) = MethodCanceledError(
        workflowName = WorkflowName(e.workflowName),
        workflowId = WorkflowId(e.workflowId),
        workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
    )
  }
}

/** Error occurring when waiting a failed task */
@Serializable
@SerialName("FailedTaskError")
data class TaskFailedError(
  /** Name of failed task */
  @SerialName("taskName") @AvroName("taskName")
  val serviceName: ServiceName,

  /** Method of failed task */
  @SerialName("methodName") @AvroName("methodName")
  val taskName: MethodName,

  /** Id of failed task */
  val taskId: TaskId,

  /** cause of the error */
  val cause: ExecutionError
) : DeferredFailedError() {
  companion object {
    fun from(e: TaskFailedException) = TaskFailedError(
        serviceName = ServiceName(e.serviceName),
        taskName = MethodName(e.methodName),
        taskId = TaskId(e.taskId),
        cause = ExecutionError.from(e.workerException),
    )
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowError")
data class MethodFailedError(
  /** Name of failed child workflow */
  val workflowName: WorkflowName,

  /** Method of failed child workflow */
  @AvroName("methodName") val workflowMethodName: MethodName,

  /** Id of failed child workflow */
  val workflowId: WorkflowId,

  /** Id of failed method run */
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId?,

  /** error */
  val deferredError: DeferredError
) : DeferredFailedError() {
  companion object {
    fun from(e: WorkflowFailedException): MethodFailedError = MethodFailedError(
        workflowName = WorkflowName(e.workflowName),
        workflowId = WorkflowId(e.workflowId),
        workflowMethodName = MethodName(e.workflowMethodName),
        workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
        deferredError = from(e.deferredException),
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
    fun from(e: WorkflowTaskFailedException): WorkflowTaskFailedError =
        WorkflowTaskFailedError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowTaskId = TaskId(e.workflowTaskId),
            cause = ExecutionError.from(e.workerException),
        )
  }
}
