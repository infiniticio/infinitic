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
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.TaskUnknownError
import io.infinitic.common.tasks.executors.errors.WorkflowCanceledError
import io.infinitic.common.tasks.executors.errors.WorkflowFailedError
import io.infinitic.common.tasks.executors.errors.WorkflowTaskFailedError
import io.infinitic.common.tasks.executors.errors.WorkflowTimedOutError
import io.infinitic.common.tasks.executors.errors.WorkflowUnknownError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DeferredException : kotlin.RuntimeException() {
  companion object {
    fun from(error: DeferredError) =
        when (error) {
          is DeferredUnknownError -> UnknownDeferredException.from(error)
          is DeferredTimedOutError -> TimedOutDeferredException.from(error)
          is DeferredCanceledError -> CanceledDeferredException.from(error)
          is DeferredFailedError -> FailedDeferredException.from(error)
        }
  }
}

@Serializable
sealed class UnknownDeferredException : DeferredException() {
  companion object {
    fun from(error: DeferredUnknownError) =
        when (error) {
          is TaskUnknownError -> UnknownTaskException.from(error)
          is WorkflowUnknownError -> UnknownWorkflowException.from(error)
        }
  }
}

@Serializable
sealed class TimedOutDeferredException : DeferredException() {
  companion object {
    fun from(error: DeferredTimedOutError) =
        when (error) {
          is TaskTimedOutError -> TaskTimedOutException.from(error)
          is WorkflowTimedOutError -> WorkflowTimedOutException.from(error)
        }
  }
}

@Serializable
sealed class CanceledDeferredException : DeferredException() {
  companion object {
    fun from(error: DeferredCanceledError) =
        when (error) {
          is TaskCanceledError -> TaskCanceledException.from(error)
          is WorkflowCanceledError -> WorkflowCanceledException.from(error)
        }
  }
}

@Serializable
sealed class FailedDeferredException : DeferredException() {
  companion object {
    fun from(error: DeferredFailedError) =
        when (error) {
          is TaskFailedError -> TaskFailedException.from(error)
          is WorkflowFailedError -> WorkflowFailedException.from(error)
          is WorkflowTaskFailedError -> WorkflowTaskFailedException.from(error)
        }
  }
}

/** Exception occurring when waiting for an unknown task */
@Serializable
data class UnknownTaskException(
  /** Name of the canceled task */
  @SerialName("taskName") val serviceName: String,

  /** Id of the canceled task */
  val taskId: String
) : UnknownDeferredException() {
  companion object {
    fun from(error: TaskUnknownError) =
        UnknownTaskException(
            serviceName = error.serviceName.toString(), taskId = error.taskId.toString(),
        )
  }
}

/** Exception occurring when waiting for an unknown workflow */
@Serializable
data class UnknownWorkflowException(
  /** Name of the canceled child workflow */
  val workflowName: String,

  /** Id of the canceled child workflow */
  val workflowId: String,

  /** Id of the methodRun */
  val methodRunId: String?
) : UnknownDeferredException() {
  companion object {
    fun from(error: WorkflowUnknownError) =
        UnknownWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodRunId = error.methodRunId?.toString(),
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
) : TimedOutDeferredException() {
  companion object {
    fun from(error: TaskTimedOutError) =
        TaskTimedOutException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString(),
        )
  }
}

/** Error occurring when waiting for an timed-out workflow */
@Serializable
data class WorkflowTimedOutException(
  /** Name of the canceled child workflow */
  val workflowName: String,

  /** Id of the canceled child workflow */
  val workflowId: String,

  /** Method called */
  val methodName: String,

  /** Id of the methodRun */
  val methodRunId: String?
) : TimedOutDeferredException() {
  companion object {
    fun from(error: WorkflowTimedOutError) =
        WorkflowTimedOutException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodName = error.methodName.toString(),
            methodRunId = error.methodRunId?.toString(),
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
) : CanceledDeferredException() {
  companion object {
    fun from(error: TaskCanceledError) =
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

  /** Id of the methodRun */
  val methodRunId: String?
) : CanceledDeferredException() {
  companion object {
    fun from(error: WorkflowCanceledError) =
        WorkflowCanceledException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodRunId = error.methodRunId?.toString(),
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
) : FailedDeferredException() {
  companion object {
    fun from(error: TaskFailedError) =
        TaskFailedException(
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
  val methodName: String,

  /** Id of the methodRun */
  val methodRunId: String?,

  /** cause of the error */
  val deferredException: DeferredException
) : FailedDeferredException() {
  companion object {
    fun from(error: WorkflowFailedError): WorkflowFailedException =
        WorkflowFailedException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodName = error.methodName.toString(),
            methodRunId = error.methodRunId.toString(),
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
) : FailedDeferredException() {
  companion object {
    fun from(error: WorkflowTaskFailedError) =
        WorkflowTaskFailedException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowTaskId = error.workflowTaskId.toString(),
            workerException = WorkerException.from(error.cause),
        )
  }
}
