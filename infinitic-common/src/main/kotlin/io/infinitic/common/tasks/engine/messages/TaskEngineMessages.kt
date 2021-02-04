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

package io.infinitic.common.tasks.engine.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskRetry
import io.infinitic.common.tasks.engine.messages.interfaces.FailingTaskAttemptMessage
import io.infinitic.common.tasks.engine.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable

@Serializable
sealed class TaskEngineMessage() {
    val messageId = MessageId()
    abstract val taskId: TaskId
}

@Serializable
data class DispatchTask(
    override val taskId: TaskId,
    val clientName: ClientName?,
    val taskName: TaskName,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodInput: MethodInput,
    val workflowId: WorkflowId?,
    val methodRunId: MethodRunId?,
    val taskMeta: TaskMeta,
    val taskOptions: TaskOptions = TaskOptions()
) : TaskEngineMessage()

@Serializable
data class RetryTask(
    override val taskId: TaskId,
    val taskName: TaskName?,
    val methodName: MethodName?,
    val methodParameterTypes: MethodParameterTypes?,
    val methodInput: MethodInput?,
    val taskMeta: TaskMeta?,
    val taskOptions: TaskOptions?
) : TaskEngineMessage()

@Serializable
data class CancelTask(
    override val taskId: TaskId,
    val taskOutput: MethodOutput
) : TaskEngineMessage()

@Serializable
data class TaskCanceled(
    override val taskId: TaskId,
    val taskOutput: MethodOutput,
    val taskMeta: TaskMeta
) : TaskEngineMessage()

@Serializable
data class TaskCompleted(
    override val taskId: TaskId,
    val taskName: TaskName,
    val taskOutput: MethodOutput,
    val taskMeta: TaskMeta
) : TaskEngineMessage()

@Serializable
data class RetryTaskAttempt(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskRetry: TaskRetry
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskRetry: TaskRetry
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskRetry: TaskRetry
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskRetry: TaskRetry,
    val taskOutput: MethodOutput
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskRetry: TaskRetry,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : TaskEngineMessage(), FailingTaskAttemptMessage
