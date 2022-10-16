/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.exceptions

import io.infinitic.common.tasks.executors.errors.CanceledDeferredError
import io.infinitic.common.tasks.executors.errors.CanceledTaskError
import io.infinitic.common.tasks.executors.errors.CanceledWorkflowError
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.FailedDeferredError
import io.infinitic.common.tasks.executors.errors.FailedTaskError
import io.infinitic.common.tasks.executors.errors.FailedWorkflowError
import io.infinitic.common.tasks.executors.errors.FailedWorkflowTaskError
import io.infinitic.common.tasks.executors.errors.TimedOutDeferredError
import io.infinitic.common.tasks.executors.errors.TimedOutTaskError
import io.infinitic.common.tasks.executors.errors.TimedOutWorkflowError
import io.infinitic.common.tasks.executors.errors.UnknownDeferredError
import io.infinitic.common.tasks.executors.errors.UnknownTaskError
import io.infinitic.common.tasks.executors.errors.UnknownWorkflowError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DeferredException : kotlin.RuntimeException() {
    companion object {
        fun from(error: DeferredError) = when (error) {
            is UnknownDeferredError -> UnknownDeferredException.from(error)
            is TimedOutDeferredError -> TimedOutDeferredException.from(error)
            is CanceledDeferredError -> CanceledDeferredException.from(error)
            is FailedDeferredError -> FailedDeferredException.from(error)
        }
    }
}

@Serializable
sealed class UnknownDeferredException : DeferredException() {
    companion object {
        fun from(error: UnknownDeferredError) = when (error) {
            is UnknownTaskError -> UnknownTaskException.from(error)
            is UnknownWorkflowError -> UnknownWorkflowException.from(error)
        }
    }
}

@Serializable
sealed class TimedOutDeferredException : DeferredException() {
    companion object {
        fun from(error: TimedOutDeferredError) = when (error) {
            is TimedOutTaskError -> TimedOutTaskException.from(error)
            is TimedOutWorkflowError -> TimedOutWorkflowException.from(error)
        }
    }
}

@Serializable
sealed class CanceledDeferredException : DeferredException() {
    companion object {
        fun from(error: CanceledDeferredError) = when (error) {
            is CanceledTaskError -> CanceledTaskException.from(error)
            is CanceledWorkflowError -> CanceledWorkflowException.from(error)
        }
    }
}

@Serializable
sealed class FailedDeferredException : DeferredException() {
    companion object {
        fun from(error: FailedDeferredError) = when (error) {
            is FailedTaskError -> FailedTaskException.from(error)
            is FailedWorkflowError -> FailedWorkflowException.from(error)
            is FailedWorkflowTaskError -> FailedWorkflowTaskException.from(error)
        }
    }
}

/**
 * Exception occurring when waiting for an unknown task
 */
@Serializable
data class UnknownTaskException(
    /**
     * Name of the canceled task
     */
    @SerialName("taskName") val serviceName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String
) : UnknownDeferredException() {
    companion object {
        fun from(error: UnknownTaskError) = UnknownTaskException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString()
        )
    }
}

/**
 * Exception occurring when waiting for an unknown workflow
 */
@Serializable
data class UnknownWorkflowException(
    /**
     * Name of the canceled child workflow
     */
    val workflowName: String,

    /**
     * Id of the canceled child workflow
     */
    val workflowId: String,

    /**
     * Id of the methodRun
     */
    val methodRunId: String?
) : UnknownDeferredException() {
    companion object {
        fun from(error: UnknownWorkflowError) = UnknownWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodRunId = error.methodRunId?.toString()
        )
    }
}

/**
 * Exception occurring when waiting for a timed-out task
 */
@Serializable
data class TimedOutTaskException(
    /**
     * Name of the canceled task
     */
    @SerialName("taskName") val serviceName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String,

    /**
     * Method called
     */
    val methodName: String

) : TimedOutDeferredException() {
    companion object {
        fun from(error: TimedOutTaskError) = TimedOutTaskException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString()
        )
    }
}

/**
 * Error occurring when waiting for an timed-out workflow
 */
@Serializable
data class TimedOutWorkflowException(
    /**
     * Name of the canceled child workflow
     */
    val workflowName: String,

    /**
     * Id of the canceled child workflow
     */
    val workflowId: String,

    /**
     * Method called
     */
    val methodName: String,

    /**
     * Id of the methodRun
     */
    val methodRunId: String?
) : TimedOutDeferredException() {
    companion object {
        fun from(error: TimedOutWorkflowError) = TimedOutWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodName = error.methodName.toString(),
            methodRunId = error.methodRunId?.toString()
        )
    }
}

/**
 * Exception occurring when waiting for a canceled task
 */
@Serializable
data class CanceledTaskException(
    /**
     * Name of the canceled task
     */
    @SerialName("taskName") val serviceName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String,

    /**
     * Method called
     */
    val methodName: String

) : CanceledDeferredException() {
    companion object {
        fun from(error: CanceledTaskError) = CanceledTaskException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString()
        )
    }
}

/**
 * Exception occurring when waiting for a canceled workflow
 */
@Serializable
data class CanceledWorkflowException(
    /**
     * Name of the canceled child workflow
     */
    val workflowName: String,

    /**
     * Id of the canceled child workflow
     */
    val workflowId: String,

    /**
     * Id of the methodRun
     */
    val methodRunId: String?
) : CanceledDeferredException() {
    companion object {
        fun from(error: CanceledWorkflowError) = CanceledWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodRunId = error.methodRunId?.toString()
        )
    }
}

/**
 * Exception occurring when waiting fora failed task
 */
@Serializable
data class FailedTaskException(
    /**
     * Name of the task where the error occurred
     */
    @SerialName("taskName") val serviceName: String,

    /**
     * Id of the task where the error occurred
     */
    val taskId: String,

    /**
     * Method called where the error occurred
     */
    val methodName: String,

    /**
     * cause of the error
     */
    val workerException: WorkerException
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedTaskError) = FailedTaskException(
            serviceName = error.serviceName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString(),
            workerException = WorkerException.from(error.cause)
        )
    }
}

/**
 * Exception occurring when waiting fora failed task
 */
@Serializable
data class FailedWorkflowException(
    /**
     * Name of the workflow where the error occurred
     */
    val workflowName: String,

    /**
     * Id of the workflow where the error occurred
     */
    val workflowId: String,

    /**
     * Method called where the error occurred
     */
    val methodName: String,

    /**
     * Id of the methodRun
     */
    val methodRunId: String?,

    /**
     * cause of the error
     */
    val deferredException: DeferredException
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedWorkflowError): FailedWorkflowException = FailedWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodName = error.methodName.toString(),
            methodRunId = error.methodRunId.toString(),
            deferredException = from(error.deferredError)
        )
    }
}

/**
 * Exception occurred during a workflow task
 */
@Serializable
data class FailedWorkflowTaskException(
    /**
     * Name of the workflow for which the error occurred
     */
    val workflowName: String,

    /**
     * Id of the workflow for which the error occurred
     */
    val workflowId: String,

    /**
     * Id of the workflow task for which the error occurred
     */
    val workflowTaskId: String,

    /**
     * cause of the error
     */
    val workerException: WorkerException
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedWorkflowTaskError) = FailedWorkflowTaskException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowTaskId = error.workflowTaskId.toString(),
            workerException = WorkerException.from(error.cause)
        )
    }
}
