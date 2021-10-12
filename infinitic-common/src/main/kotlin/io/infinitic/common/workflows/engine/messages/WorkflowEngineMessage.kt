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

package io.infinitic.common.workflows.engine.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.errors.Error
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.interfaces.MethodRunMessage
import io.infinitic.common.workflows.engine.messages.interfaces.TaskMessage
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowEngineMessage : Message {
    val messageId: MessageId = MessageId()
    abstract val emitterName: ClientName
    abstract val workflowId: WorkflowId
    abstract val workflowName: WorkflowName

    override fun envelope() = WorkflowEngineEnvelope.from(this)

    fun isWorkflowTask() = (this is TaskMessage) && this.taskName == TaskName(WorkflowTask::class.java.name)
}

@Serializable
data class DispatchWorkflow(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val methodName: MethodName,
    val methodParameters: MethodParameters,
    val methodParameterTypes: MethodParameterTypes?,
    val workflowOptions: WorkflowOptions,
    val workflowTags: Set<WorkflowTag>,
    val workflowMeta: WorkflowMeta,
    var parentWorkflowName: WorkflowName?,
    var parentWorkflowId: WorkflowId?,
    var parentMethodRunId: MethodRunId?,
    val clientWaiting: Boolean,
    override val emitterName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class DispatchMethod(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val methodName: MethodName,
    val methodParameters: MethodParameters,
    val methodParameterTypes: MethodParameterTypes?,
    var parentWorkflowId: WorkflowId?,
    var parentWorkflowName: WorkflowName?,
    var parentMethodRunId: MethodRunId?,
    val clientWaiting: Boolean,
    override val emitterName: ClientName,
) : WorkflowEngineMessage()

@Serializable
data class WaitWorkflow(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId?,
    override val emitterName: ClientName,
) : WorkflowEngineMessage()

@Serializable
data class CancelWorkflow(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId?,
    val reason: WorkflowCancellationReason,
    override val emitterName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class RetryWorkflowTask(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val emitterName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class CompleteWorkflow(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val workflowReturnValue: MethodReturnValue,
    override val emitterName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class SendSignal(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    val channelName: ChannelName,
    val channelSignalId: ChannelSignalId,
    val channelSignal: ChannelSignal,
    val channelSignalTypes: Set<ChannelSignalType>,
    override val emitterName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class ChildMethodCanceled(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    val childWorkflowName: WorkflowName,
    val childWorkflowId: WorkflowId,
    val childMethodRunId: MethodRunId,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodRunMessage

@Serializable
data class ChildMethodFailed(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childMethodRunId: MethodRunId,
    val childWorkflowError: Error,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodRunMessage

@Serializable
data class ChildMethodCompleted(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childMethodRunId: MethodRunId,
    val childWorkflowReturnValue: MethodReturnValue,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodRunMessage

@Serializable
data class TimerCompleted(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    val timerId: TimerId,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodRunMessage

@Serializable
data class TaskFailed(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    override val taskName: TaskName,
    override val taskId: TaskId,
    val taskError: Error,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskMessage, MethodRunMessage

@Serializable
data class TaskCanceled(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskMessage, MethodRunMessage

@Serializable
data class TaskCompleted(
    override val workflowName: WorkflowName,
    override val workflowId: WorkflowId,
    override val methodRunId: MethodRunId,
    override val taskName: TaskName,
    override val taskId: TaskId,
    val taskReturnValue: MethodReturnValue,
    override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskMessage, MethodRunMessage
