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
package io.infinitic.exceptions

import io.infinitic.common.tasks.executors.errors.DeferredCanceledError
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.DeferredFailedError
import io.infinitic.common.tasks.executors.errors.DeferredTimedOutError
import io.infinitic.common.tasks.executors.errors.DeferredUnknownError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * DeferredException are use-facing exceptions.
 *
 * They can be thrown when an exception occurs while synchronously waiting for a deferred.
 */


@Serializable
sealed class DeferredException : kotlin.RuntimeException() {
  abstract val description: String

  companion object {
    fun from(error: DeferredError) =
        when (error) {
          is DeferredUnknownError -> DeferredUnknownException.from(error)
          is DeferredTimedOutError -> DeferredTimedOutException.from(error)
          is DeferredCanceledError -> DeferredCanceledException.from(error)
          is DeferredFailedError -> DeferredFailedException.from(error)
        }
  }
}

@Serializable
sealed class DeferredUnknownException : DeferredException() {
  companion object {
    fun from(error: DeferredUnknownError) =
        when (error) {
          is TaskUnknownError -> TaskUnknownException.from(error)
          is MethodUnknownError -> WorkflowUnknownException.from(error)
        }
  }
}

@Serializable
sealed class DeferredTimedOutException : DeferredException() {
  companion object {
    fun from(error: DeferredTimedOutError) =
        when (error) {
          is TaskTimedOutError -> TaskTimedOutException.from(error)
          is MethodTimedOutError -> WorkflowTimedOutException.from(error)
        }
  }
}

@Serializable
sealed class DeferredCanceledException : DeferredException() {
  companion object {
    fun from(error: DeferredCanceledError) =
        when (error) {
          is TaskCanceledError -> TaskCanceledException.from(error)
          is MethodCanceledError -> WorkflowCanceledException.from(error)
        }
  }
}

@Serializable
sealed class DeferredFailedException : DeferredException() {
  companion object {
    fun from(error: DeferredFailedError) =
        when (error) {
          is TaskFailedError -> TaskFailedException.from(error)
          is MethodFailedError -> WorkflowFailedException.from(error)
          is WorkflowTaskFailedError -> WorkflowTaskFailedException.from(error)
        }
  }
}

/** Exception occurring when waiting for an unknown task */
@Serializable
data class TaskUnknownException(
  /** Name of the canceled task */
  @SerialName("taskName") val serviceName: String,

  val methodName: String? = null,

  /** Id of the canceled task */
  val taskId: String
) : DeferredUnknownException() {
  @Transient
  override val description = "Unable to fetch the result of a remote task. " +
      "It appears this task has either already terminated or is not recognized " +
      "(serviceName: $serviceName, " +
      (methodName?.let { "methodName: $methodName, " } ?: "") +
      "taskId: $taskId)."

  companion object {
    fun from(error: TaskUnknownError): TaskUnknownException =
        TaskUnknownException(
            serviceName = error.serviceName.toString(),
            methodName = error.methodName?.let { toString() },
            taskId = error.taskId.toString(),
        )
  }
}

/** Exception occurring when waiting for an unknown workflow */
@Serializable
data class WorkflowUnknownException(
  /** Name of the canceled child workflow */
  val workflowName: String,

  /** Id of the canceled child workflow */
  val workflowId: String,

  val workflowMethodName: String?,

  /** Id of the methodRun */
  val workflowMethodId: String?
) : DeferredUnknownException() {
  @Transient
  override val description = "Unable to fetch the result of a remote workflow. " +
      "It appears this workflow has either already terminated or is not recognized " +
      "(workflowName: $workflowName, workflowId: $workflowId" +
      (workflowMethodName?.let { ", methodName: $it" } ?: "") +
      (workflowMethodId?.let { ", methodId: $it" } ?: "") +
      ".)"

  companion object {
    fun from(error: MethodUnknownError): WorkflowUnknownException =
        WorkflowUnknownException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowMethodName = error.workflowMethodName?.toString(),
            workflowMethodId = error.workflowMethodId?.toString(),
        )
  }
}

/** Exception occurring when waiting for a timed-out task */
@Serializable
data class TaskTimedOutException(
  /** Name of the canceled task */
  @SerialName("taskName") val serviceName: String,

  /** Id of the canceled task */
  val taskId: String,

  /** Method called */
  val methodName: String
) : DeferredTimedOutException() {
  @Transient
  override val description = "Unable to fetch the result of a remote task. " +
      "The time allotted for this task has expired. " +
      "(Service Name: $serviceName, Method Name: $methodName, Task ID: $taskId)"

  companion object {
    fun from(error: TaskTimedOutError): TaskTimedOutException =
        TaskTimedOutException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString(),
        )
  }
}

/** Error occurring when waiting for a timed-out workflow */
@Serializable
data class WorkflowTimedOutException(
  /** Name of the canceled child workflow */
  val workflowName: String,

  /** Id of the canceled child workflow */
  val workflowId: String,

  val workflowMethodName: String,

  /** Id of the methodRun */
  val workflowMethodId: String?
) : DeferredTimedOutException() {
  @Transient
  override val description = "Unable to fetch the result of a remote workflow. " +
      "The time allotted for this workflow has expired. " +
      "(Workflow Name: $workflowName, Workflow ID: $workflowId, Method Name: $workflowMethodName" +
      (workflowMethodId?.let { ", Method ID: $it" } ?: "") +
      ")."

  companion object {
    fun from(error: MethodTimedOutError): WorkflowTimedOutException =
        WorkflowTimedOutException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowMethodName = error.workflowMethodName.toString(),
            workflowMethodId = error.workflowMethodId?.toString(),
        )
  }
}

/** Exception occurring when waiting for a canceled task */
@Serializable
data class TaskCanceledException(
  /** Name of the canceled task */
  @SerialName("taskName") val serviceName: String,

  /** Id of the canceled task */
  val taskId: String,

  /** Method called */
  val methodName: String
) : DeferredCanceledException() {
  @Transient
  override val description = "Unable to fetch the result of a remote task. " +
      "It appears this task has been canceled " +
      "(Service Name: $serviceName, Method Name: $methodName, Task ID: $taskId)"

  companion object {
    fun from(error: TaskCanceledError): TaskCanceledException =
        TaskCanceledException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString(),
        )
  }
}

/** Exception occurring when waiting for a canceled workflow */
@Serializable
data class WorkflowCanceledException(
  /** Name of the canceled child workflow */
  val workflowName: String,

  /** Id of the canceled child workflow */
  val workflowId: String,

  val workflowMethodName: String?,

  /** Id of the methodRun */
  val workflowMethodId: String?
) : DeferredCanceledException() {
  @Transient
  override val description = "Unable to fetch the result of a remote workflow. " +
      "It appears this workflow has been canceled " +
      "(Workflow Name: $workflowName, Workflow ID: $workflowId" +
      (workflowMethodName?.let { ", Method Name: $it" } ?: "") +
      (workflowMethodId?.let { ", Method ID: $it" } ?: "") +
      ")."

  companion object {
    fun from(error: MethodCanceledError): WorkflowCanceledException =
        WorkflowCanceledException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowMethodName = error.workflowMethodName?.toString(),
            workflowMethodId = error.workflowMethodId?.toString(),
        )
  }
}

/** Exception occurring when waiting fora failed task */
@Serializable
data class TaskFailedException(
  /** Name of the task where the error occurred */
  @SerialName("taskName") val serviceName: String,

  /** Id of the task where the error occurred */
  val taskId: String,

  /** Method called where the error occurred */
  val methodName: String,

  /** cause of the error */
  val workerException: WorkerException
) : DeferredFailedException() {
  @Transient
  override val description = "Unable to fetch the result of a remote task. " +
      "It appears this task has failed " +
      "(Service Name: $serviceName, Method Name: $methodName, Task ID: $taskId)"

  companion object {
    fun from(error: TaskFailedError) = TaskFailedException(
        serviceName = error.serviceName.toString(),
        taskId = error.taskId.toString(),
        methodName = error.methodName.toString(),
        workerException = WorkerException.from(error.cause),
    )
  }
}

/** Exception occurring when waiting fora failed task */
@Serializable
data class WorkflowFailedException(
  /** Name of the workflow where the error occurred */
  val workflowName: String,

  /** Id of the workflow where the error occurred */
  val workflowId: String,

  /** Method called where the error occurred */
  val workflowMethodName: String,

  /** Id of the methodRun */
  val workflowMethodId: String?,

  /** cause of the error */
  val deferredException: DeferredException
) : DeferredFailedException() {
  @Transient
  override val description = "Unable to fetch the result of a remote workflow. " +
      "It appears this workflow has failed " +
      "(Workflow Name: $workflowName, Workflow ID: $workflowId, Method Name: $workflowMethodName" +
      (workflowMethodId?.let { ", Method ID: $it" } ?: "") +
      ")."

  companion object {
    fun from(error: MethodFailedError): WorkflowFailedException =
        WorkflowFailedException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowMethodName = error.workflowMethodName.toString(),
            workflowMethodId = error.workflowMethodId.toString(),
            deferredException = from(error.deferredError),
        )
  }
}

/** Exception occurred during a workflow task */
@Serializable
data class WorkflowTaskFailedException(
  /** Name of the workflow for which the error occurred */
  val workflowName: String,

  /** Id of the workflow for which the error occurred */
  val workflowId: String,

  /** Id of the workflow task for which the error occurred */
  val workflowTaskId: String,

  /** cause of the error */
  val workerException: WorkerException
) : DeferredFailedException() {
  @Transient
  override val description =
      "Unable to continue the execution of this workflow. An exception has raised."

  companion object {
    fun from(error: WorkflowTaskFailedError): WorkflowTaskFailedException =
        WorkflowTaskFailedException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowTaskId = error.workflowTaskId.toString(),
            workerException = WorkerException.from(error.cause),
        )
  }
}
