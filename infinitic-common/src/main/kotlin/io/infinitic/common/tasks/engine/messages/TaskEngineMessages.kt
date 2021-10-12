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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.errors.Error
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.messages.interfaces.FailingTaskAttemptMessage
import io.infinitic.common.tasks.engine.messages.interfaces.TaskAttemptMessage
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
sealed class TaskEngineMessage : Message {
    val messageId = MessageId()
    abstract val emitterName: ClientName
    abstract val taskId: TaskId
    abstract val taskName: TaskName

    override fun envelope() = TaskEngineEnvelope.from(this)
}

@Serializable
data class DispatchTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    val taskOptions: TaskOptions,
    val clientWaiting: Boolean,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val workflowId: WorkflowId?,
    val workflowName: WorkflowName?,
    val methodRunId: MethodRunId?,
    val taskTags: Set<TaskTag>,
    val taskMeta: TaskMeta,
    override val emitterName: ClientName
) : TaskEngineMessage()

@Serializable
data class WaitTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val emitterName: ClientName,
) : TaskEngineMessage()

@Serializable
data class RetryTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val emitterName: ClientName
) : TaskEngineMessage()

@Serializable
data class CancelTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val emitterName: ClientName
) : TaskEngineMessage()

@Serializable
data class CompleteTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    val taskReturnValue: MethodReturnValue,
    override val emitterName: ClientName
) : TaskEngineMessage()

@Serializable
data class RetryTaskAttempt(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val taskRetryIndex: TaskRetryIndex,
    override val taskAttemptId: TaskAttemptId,
    override val taskRetrySequence: TaskRetrySequence,
    override val emitterName: ClientName
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptCompleted(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val taskRetryIndex: TaskRetryIndex,
    override val taskAttemptId: TaskAttemptId,
    override val taskRetrySequence: TaskRetrySequence,
    val taskReturnValue: MethodReturnValue,
    val taskMeta: TaskMeta,
    override val emitterName: ClientName
) : TaskEngineMessage(), TaskAttemptMessage

@Serializable
data class TaskAttemptFailed(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val taskAttemptDelayBeforeRetry: MillisDuration?,
    override val taskAttemptId: TaskAttemptId,
    override val taskRetrySequence: TaskRetrySequence,
    override val taskRetryIndex: TaskRetryIndex,
    val taskAttemptError: Error,
    val taskMeta: TaskMeta,
    override val emitterName: ClientName
) : TaskEngineMessage(), FailingTaskAttemptMessage
