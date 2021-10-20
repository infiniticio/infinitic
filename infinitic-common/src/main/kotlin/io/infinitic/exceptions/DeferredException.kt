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

import io.infinitic.common.errors.CanceledDeferredError
import io.infinitic.common.errors.CanceledTaskError
import io.infinitic.common.errors.CanceledWorkflowError
import io.infinitic.common.errors.DeferredError
import io.infinitic.common.errors.FailedDeferredError
import io.infinitic.common.errors.FailedTaskError
import io.infinitic.common.errors.FailedWorkflowError
import io.infinitic.common.errors.FailedWorkflowTaskError
import io.infinitic.common.errors.TimedOutDeferredError
import io.infinitic.common.errors.TimedOutTaskError
import io.infinitic.common.errors.TimedOutWorkflowError
import io.infinitic.common.errors.UnknownDeferredError
import io.infinitic.common.errors.UnknownTaskError
import io.infinitic.common.errors.UnknownWorkflowError

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

sealed class UnknownDeferredException : DeferredException() {
    companion object {
        fun from(error: UnknownDeferredError) = when (error) {
            is UnknownTaskError -> UnknownTaskException.from(error)
            is UnknownWorkflowError -> UnknownWorkflowException.from(error)
        }
    }
}

sealed class TimedOutDeferredException : DeferredException() {
    companion object {
        fun from(error: TimedOutDeferredError) = when (error) {
            is TimedOutTaskError -> TimedOutTaskException.from(error)
            is TimedOutWorkflowError -> TimedOutWorkflowException.from(error)
        }
    }
}

sealed class CanceledDeferredException : DeferredException() {
    companion object {
        fun from(error: CanceledDeferredError) = when (error) {
            is CanceledTaskError -> CanceledTaskException.from(error)
            is CanceledWorkflowError -> CanceledWorkflowException.from(error)
        }
    }
}

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
class UnknownTaskException(
    /**
     * Name of the canceled task
     */
    val taskName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String,
) : UnknownDeferredException() {
    companion object {
        fun from(error: UnknownTaskError) = UnknownTaskException(
            taskName = error.taskName.toString(),
            taskId = error.taskId.toString()
        )
    }
}

/**
 * Exception occurring when waiting for an unknown workflow
 */
class UnknownWorkflowException(
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
class TimedOutTaskException(
    /**
     * Name of the canceled task
     */
    val taskName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String,

    /**
     * Method called
     */
    val methodName: String,

) : TimedOutDeferredException() {
    companion object {
        fun from(error: TimedOutTaskError) = TimedOutTaskException(
            taskName = error.taskName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString()
        )
    }
}

/**
 * Error occurring when waiting for an timed-out workflow
 */
class TimedOutWorkflowException(
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
            methodRunId = error.methodRunId?.toString(),
        )
    }
}

/**
 * Exception occurring when waiting for a canceled task
 */
class CanceledTaskException(
    /**
     * Name of the canceled task
     */
    val taskName: String,

    /**
     * Id of the canceled task
     */
    val taskId: String,

    /**
     * Method called
     */
    val methodName: String,

) : CanceledDeferredException() {
    companion object {
        fun from(error: CanceledTaskError) = CanceledTaskException(
            taskName = error.taskName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString()
        )
    }
}

/**
 * Exception occurring when waiting for a canceled workflow
 */
class CanceledWorkflowException(
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
            methodRunId = error.methodRunId?.toString(),
        )
    }
}

/**
 * Exception occurring when waiting fora failed task
 */
class FailedTaskException(
    /**
     * Name of the task where the error occurred
     */
    val taskName: String,

    /**
     * Id of the task where the error occurred
     */
    val taskId: String,

    /**
     * Method called where the error occurred
     */
    val methodName: String,

    /**
     * Name of the error
     */
    val name: String,

    /**
     * Message of the error
     */
    override val message: String?,

    /**
     * String version of the stack trace
     */
    val stackTraceToString: String,

    /**
     * cause of the error
     */
    override val cause: RuntimeException?
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedTaskError) = FailedTaskException(
            taskName = error.taskName.toString(),
            taskId = error.taskId.toString(),
            methodName = error.methodName.toString(),
            name = error.name,
            message = error.message,
            stackTraceToString = error.stackTraceToString,
            cause = error.cause?.let { RuntimeException.from(it) }
        )
    }
}

/**
 * Exception occurring when waiting fora failed task
 */
class FailedWorkflowException(
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
    override val cause: DeferredException?
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedWorkflowError): FailedWorkflowException = FailedWorkflowException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            methodName = error.methodName.toString(),
            methodRunId = error.methodRunId.toString(),
            cause = error.cause?.let { from(it) }
        )
    }
}

/**
 * Exception occurred during a workflow task
 */
class FailedWorkflowTaskException(
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
     * Name of the error
     */
    val name: String,

    /**
     * Message of the error
     */
    override val message: String?,

    /**
     * String version of the stack trace
     */
    val stackTraceToString: String,

    /**
     * cause of the error
     */
    override val cause: RuntimeException?
) : FailedDeferredException() {
    companion object {
        fun from(error: FailedWorkflowTaskError) = FailedWorkflowTaskException(
            workflowName = error.workflowName.toString(),
            workflowId = error.workflowId.toString(),
            workflowTaskId = error.workflowTaskId.toString(),
            name = error.name,
            message = error.message,
            stackTraceToString = error.stackTraceToString,
            cause = error.cause?.let { RuntimeException.from(it) }
        )
    }
}
