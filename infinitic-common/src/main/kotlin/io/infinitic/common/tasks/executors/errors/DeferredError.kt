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

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
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
import io.infinitic.exceptions.WorkflowExecutorException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.exceptions.WorkflowUnknownException
import io.infinitic.tasks.TaskFailure
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DeferredError are internal representation of [DeferredException] used to serialize and transport them
 */

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
          is WorkflowFailedException -> MethodFailedError.from(exception)
          is WorkflowExecutorException -> WorkflowExecutorError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredCanceledError : DeferredError() {
  companion object {
    fun from(exception: DeferredCanceledException) =
        when (exception) {
          is TaskCanceledException -> TaskCanceledError.from(exception)
          is WorkflowCanceledException -> MethodCanceledError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredTimedOutError : DeferredError() {
  companion object {
    fun from(exception: DeferredTimedOutException) =
        when (exception) {
          is TaskTimedOutException -> TaskTimedOutError.from(exception)
          is WorkflowTimedOutException -> MethodTimedOutError.from(exception)
        }
  }
}

@Serializable
sealed class DeferredUnknownError : DeferredError() {
  companion object {
    fun from(exception: DeferredUnknownException) =
        when (exception) {
          is TaskUnknownException -> TaskUnknownError.from(exception)
          is WorkflowUnknownException -> MethodUnknownError.from(exception)
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

  @AvroDefault(Avro.NULL) val methodName: MethodName?,

  /** ID of the unknown task */
  val taskId: TaskId
) : DeferredUnknownError() {
  companion object {
    fun from(exception: TaskUnknownException) =
        TaskUnknownError(
            serviceName = ServiceName(exception.serviceName),
            methodName = exception.methodName?.let { MethodName(it) },
            taskId = TaskId(exception.taskId),
        )
  }
}

/** Error occurring when waiting for an unknown workflow */
@Serializable
@SerialName("UnknownWorkflowError")
data class MethodUnknownError(
  /** Name of the unknown workflow */
  val workflowName: WorkflowName,

  /** ID of the unknown workflow */
  val workflowId: WorkflowId,

  @AvroDefault(Avro.NULL)
  val workflowMethodName: MethodName?,

  /** ID of the unknown workflow' method run */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?

) : DeferredUnknownError() {
  companion object {
    fun from(e: WorkflowUnknownException) =
        MethodUnknownError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowMethodName = e.workflowMethodName?.let { MethodName(it) },
            workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
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

  /** ID of the timed-out task */
  val taskId: TaskId,

  /** Method of the timed-out task */
  val methodName: MethodName
) : DeferredTimedOutError() {
  companion object {
    fun from(e: TaskTimedOutException) =
        TaskTimedOutError(
            serviceName = ServiceName(e.serviceName),
            taskId = TaskId(e.taskId),
            methodName = MethodName(e.methodName),
        )
  }
}

/** Error occurring when waiting a timed-out child workflow */
@Serializable
@SerialName("TimedOutWorkflowError")
@AvroName("TimedOutWorkflowError")
data class MethodTimedOutError(
  /** Name of timed-out child workflow */
  val workflowName: WorkflowName,

  /** ID of timed-out child workflow */
  val workflowId: WorkflowId,

  /** Method of timed-out child workflow */
  @AvroName("methodName")
  val workflowMethodName: MethodName,

  /** ID of the methodRun */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?
) : DeferredTimedOutError() {
  companion object {
    fun from(e: WorkflowTimedOutException) =
        MethodTimedOutError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowMethodName = MethodName(e.workflowMethodName),
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

  /** ID of canceled task */
  val taskId: TaskId,

  /** Method of canceled task */
  val methodName: MethodName
) : DeferredCanceledError() {
  companion object {
    fun from(e: TaskCanceledException) =
        TaskCanceledError(
            serviceName = ServiceName(e.serviceName),
            taskId = TaskId(e.taskId),
            methodName = MethodName(e.methodName),
        )
  }
}

/** Error occurring when waiting a failed child workflow */
@Serializable
@SerialName("CanceledWorkflowError")
data class MethodCanceledError(
  /** Name of canceled child workflow */
  val workflowName: WorkflowName,

  /** ID of canceled child workflow */
  val workflowId: WorkflowId,

  @AvroDefault(Avro.NULL)
  val workflowMethodName: MethodName?,

  /** ID of the methodRun */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?,
) : DeferredCanceledError() {
  companion object {
    fun from(e: WorkflowCanceledException) =
        MethodCanceledError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowMethodName = e.workflowMethodName?.let { MethodName(it) },
            workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
        )
  }
}

/** Error occurring when waiting a failed task */
@Serializable
@SerialName("FailedTaskError")
data class TaskFailedError(
  /** Name of failed task */
  @SerialName("taskName") val serviceName: ServiceName,

  /** Method of failed task */
  val methodName: MethodName,

  /** ID of failed task */
  val taskId: TaskId,

  /** cause of the error */
  @SerialName("cause") val lastFailure: TaskFailure
) : DeferredFailedError() {
  companion object {
    fun from(e: TaskFailedException) =
        TaskFailedError(
            serviceName = ServiceName(e.serviceName),
            methodName = MethodName(e.methodName),
            taskId = TaskId(e.taskId),
            lastFailure = e.lastFailure,
        )
  }
}

/** Error occurring when waiting a failed workflow */
@Serializable
@SerialName("FailedWorkflowError")
data class MethodFailedError(
  /** Name of failed child workflow */
  val workflowName: WorkflowName,

  /** ID of failed child workflow */
  val workflowId: WorkflowId,

  /** Method of failed child workflow */
  @AvroName("methodName")
  val workflowMethodName: MethodName,

  /** ID of failed method run */
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?,

  /** error */
  val deferredError: DeferredError
) : DeferredFailedError() {
  companion object {
    fun from(e: WorkflowFailedException): MethodFailedError =
        MethodFailedError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowMethodName = MethodName(e.workflowMethodName),
            workflowMethodId = e.workflowMethodId?.let { WorkflowMethodId(it) },
            deferredError = from(e.deferredException),
        )
  }
}

/** Error occurring in Workflow Task  */
@Serializable
@SerialName("FailedWorkflowTaskError")
data class WorkflowExecutorError(
  /** Name of failed workflow */
  val workflowName: WorkflowName,

  /** ID of failed workflow */
  val workflowId: WorkflowId,

  /** ID of failed workflow task */
  val workflowTaskId: TaskId,

  /** cause of the error */
  @SerialName("cause") val lastFailure: TaskFailure
) : DeferredFailedError() {
  companion object {
    fun from(e: WorkflowExecutorException): WorkflowExecutorError =
        WorkflowExecutorError(
            workflowName = WorkflowName(e.workflowName),
            workflowId = WorkflowId(e.workflowId),
            workflowTaskId = TaskId(e.workflowTaskId),
            lastFailure = e.lastFailure,
        )
  }
}
