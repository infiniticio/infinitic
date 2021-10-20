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

package io.infinitic.common.errors

import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
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
sealed class DeferredError {
    companion object {
        fun from(exception: DeferredException) = when (exception) {
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
        fun from(exception: FailedDeferredException) = when (exception) {
            is FailedTaskException -> FailedTaskError.from(exception)
            is FailedWorkflowException -> FailedWorkflowError.from(exception)
            is FailedWorkflowTaskException -> FailedWorkflowTaskError.from(exception)
        }
    }
}

@Serializable
sealed class CanceledDeferredError : DeferredError() {
    companion object {
        fun from(exception: CanceledDeferredException) = when (exception) {
            is CanceledTaskException -> CanceledTaskError.from(exception)
            is CanceledWorkflowException -> CanceledWorkflowError.from(exception)
        }
    }
}

@Serializable
sealed class TimedOutDeferredError : DeferredError() {
    companion object {
        fun from(exception: TimedOutDeferredException) = when (exception) {
            is TimedOutTaskException -> TimedOutTaskError.from(exception)
            is TimedOutWorkflowException -> TimedOutWorkflowError.from(exception)
        }
    }
}

@Serializable
sealed class UnknownDeferredError : DeferredError() {
    companion object {
        fun from(exception: UnknownDeferredException) = when (exception) {
            is UnknownTaskException -> UnknownTaskError.from(exception)
            is UnknownWorkflowException -> UnknownWorkflowError.from(exception)
        }
    }
}

interface WorkflowError {
    val workflowName: WorkflowName
    val workflowId: WorkflowId
    val methodRunId: MethodRunId?
}

interface TaskErrorInterface {
    val taskName: TaskName
    val taskId: TaskId
}

/**
 * Error occurring when waiting for an unknown task
 */
@Serializable @SerialName("UnknownTaskError")
data class UnknownTaskError(
    /**
     * Name of the unknown task
     */
    override val taskName: TaskName,

    /**
     * Id of the unknown task
     */
    override val taskId: TaskId
) : UnknownDeferredError(), TaskErrorInterface {
    companion object {
        fun from(exception: UnknownTaskException) = UnknownTaskError(
            taskName = TaskName(exception.taskName),
            taskId = TaskId(exception.taskId)
        )
    }
}

/**
 * Error occurring when waiting for an unknown workflow
 */
@Serializable @SerialName("UnknownWorkflowError")
data class UnknownWorkflowError(
    /**
     * Name of the unknown workflow
     */
    override val workflowName: WorkflowName,

    /**
     * Id of the unknown workflow
     */
    override val workflowId: WorkflowId,

    /**
     * Id of the methodRun
     */
    override val methodRunId: MethodRunId?
) : UnknownDeferredError(), WorkflowError {
    companion object {
        fun from(exception: UnknownWorkflowException) = UnknownWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) }
        )
    }
}

/**
 * Error occurring when waiting a timed-out task
 */
@Serializable @SerialName("TimedOutTaskError")
data class TimedOutTaskError(
    /**
     * Name of the canceled task
     */
    override val taskName: TaskName,

    /**
     * Id of the canceled task
     */
    override val taskId: TaskId,

    /**
     * Method called
     */
    val methodName: MethodName

) : TimedOutDeferredError(), TaskErrorInterface {
    companion object {
        fun from(exception: TimedOutTaskException) = TimedOutTaskError(
            taskName = TaskName(exception.taskName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName)
        )
    }
}

/**
 * Error occurring when waiting a timed-out child workflow
 */
@Serializable @SerialName("TimedOutWorkflowError")
data class TimedOutWorkflowError(
    /**
     * Name of the canceled child workflow
     */
    override val workflowName: WorkflowName,

    /**
     * Id of the canceled child workflow
     */
    override val workflowId: WorkflowId,

    /**
     * Method called
     */
    val methodName: MethodName,

    /**
     * Id of the methodRun
     */
    override val methodRunId: MethodRunId?
) : TimedOutDeferredError(), WorkflowError {
    companion object {
        fun from(exception: TimedOutWorkflowException) = TimedOutWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) }
        )
    }
}

/**
 * Error occurring when waiting a canceled task
 */
@Serializable @SerialName("CanceledTaskError")
data class CanceledTaskError(
    /**
     * Name of the canceled task
     */
    override val taskName: TaskName,

    /**
     * Id of the canceled task
     */
    override val taskId: TaskId,

    /**
     * Method called
     */
    val methodName: MethodName,

) : CanceledDeferredError(), TaskErrorInterface {
    companion object {
        fun from(exception: CanceledTaskException) = CanceledTaskError(
            taskName = TaskName(exception.taskName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName)
        )
    }
}

/**
 * Error occurring when waiting a failed child workflow
 */
@Serializable @SerialName("CanceledWorkflowError")
data class CanceledWorkflowError(
    /**
     * Name of the canceled child workflow
     */
    override val workflowName: WorkflowName,

    /**
     * Id of the canceled child workflow
     */
    override val workflowId: WorkflowId,

    /**
     * Id of the methodRun
     */
    override val methodRunId: MethodRunId?
) : CanceledDeferredError(), WorkflowError {
    companion object {
        fun from(exception: CanceledWorkflowException) = CanceledWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) }
        )
    }
}

/**
 * Error occurring when waiting a failed task
 */
@Serializable @SerialName("FailedTaskError")
data class FailedTaskError(
    /**
     * Name of the task where the error occurred
     */
    override val taskName: TaskName,

    /**
     * Id of the task where the error occurred
     */
    override val taskId: TaskId,

    /**
     * Method called where the error occurred
     */
    val methodName: MethodName,

    /**
     * Name of the error
     */
    val name: String,

    /**
     * Message of the error
     */
    val message: String?,

    /**
     * String version of the stack trace
     */
    val stackTraceToString: String,

    /**
     * cause of the error
     */
    val cause: RuntimeError?
) : FailedDeferredError(), TaskErrorInterface {
    companion object {
        fun from(exception: FailedTaskException) = FailedTaskError(
            taskName = TaskName(exception.taskName),
            taskId = TaskId(exception.taskId),
            methodName = MethodName(exception.methodName),
            name = exception.name,
            message = exception.message,
            stackTraceToString = exception.stackTraceToString,
            cause = exception.cause?.let { RuntimeError.from(it) }
        )
    }

    // removing stackTraceToString of the output to preserve logs
    override fun toString(): String = this::class.java.simpleName + "(" + listOf(
        "taskName" to taskName,
        "taskId" to taskId,
        "methodName" to methodName,
        "name" to name,
        "message" to message,
        "cause" to cause
    ).joinToString() { "${it.first}=${it.second}" } + ")"
}

/**
 * Error occurring when waiting a failed workflow
 */
@Serializable @SerialName("FailedWorkflowError")
data class FailedWorkflowError(
    /**
     * Name of the child workflow where the error occurred
     */
    val workflowName: WorkflowName,

    /**
     * Method called where the error occurred
     */
    val methodName: MethodName,

    /**
     * Id of the child workflow where the error occurred
     */
    val workflowId: WorkflowId,

    /**
     * Id of the methodRun where the error occurred
     */
    val methodRunId: MethodRunId?,

    /**
     * error
     */
    val cause: DeferredError?
) : FailedDeferredError() {
    companion object {
        fun from(exception: FailedWorkflowException): FailedWorkflowError = FailedWorkflowError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            methodName = MethodName(exception.methodName),
            methodRunId = exception.methodRunId?.let { MethodRunId(it) },
            cause = exception.cause?.let { from(it) }
        )
    }
}

/**
 * Error occurring when waiting a failed workflow
 */
@Serializable @SerialName("FailedWorkflowTaskError")
data class FailedWorkflowTaskError(
    /**
     * Name of the child workflow where the error occurred
     */
    val workflowName: WorkflowName,

    /**
     * Id of the child workflow where the error occurred
     */
    val workflowId: WorkflowId,

    /**
     * Id of the workflow task where the error occurred
     */
    val workflowTaskId: TaskId,

    /**
     * Name of the error
     */
    val name: String,

    /**
     * Message of the error
     */
    val message: String?,

    /**
     * String version of the stack trace
     */
    val stackTraceToString: String,

    /**
     * cause of the error
     */
    val cause: RuntimeError?
) : FailedDeferredError() {
    companion object {
        fun from(exception: FailedWorkflowTaskException): FailedWorkflowTaskError = FailedWorkflowTaskError(
            workflowName = WorkflowName(exception.workflowName),
            workflowId = WorkflowId(exception.workflowId),
            workflowTaskId = TaskId(exception.workflowTaskId),
            name = exception.name,
            message = exception.message,
            stackTraceToString = exception.stackTraceToString,
            cause = exception.cause?.let { RuntimeError.from(it) }
        )
    }

    // removing stackTraceToString of the output to preserve logs
    override fun toString(): String = this::class.java.simpleName + "(" + listOf(
        "workflowName" to workflowName,
        "workflowId" to workflowId,
        "workflowTaskId" to workflowTaskId,
        "name" to name,
        "message" to message,
        "cause" to cause
    ).joinToString() { "${it.first}=${it.second}" } + ")"
}
