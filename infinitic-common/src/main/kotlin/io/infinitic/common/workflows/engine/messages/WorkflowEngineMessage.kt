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
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskError
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowEngineMessage : Message {
    val messageId: MessageId = MessageId()
    abstract val workflowId: WorkflowId
    abstract val workflowName: WorkflowName

    override fun envelope() = WorkflowEngineEnvelope.from(this)

    fun isWorkflowTaskCompleted() = (this is TaskCompleted) && this.taskName == TaskName(WorkflowTask::class.java.name)
}

@Serializable
data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val clientName: ClientName,
    val clientWaiting: Boolean,
    var parentWorkflowId: WorkflowId?,
    var parentWorkflowName: WorkflowName?,
    var parentMethodRunId: MethodRunId?,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val workflowTags: Set<WorkflowTag>,
    val workflowMeta: WorkflowMeta,
    val workflowOptions: WorkflowOptions
) : WorkflowEngineMessage()

@Serializable
data class WaitWorkflow(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val clientName: ClientName
) : WorkflowEngineMessage()

@Serializable
data class CancelWorkflow(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName
) : WorkflowEngineMessage()

@Serializable
data class CompleteWorkflow(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val workflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class SendToChannel(
    val clientName: ClientName,
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val channelEventId: ChannelEventId,
    val channelName: ChannelName,
    val channelEvent: ChannelEvent,
    val channelEventTypes: Set<ChannelEventType>
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childWorkflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class TimerCompleted(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val timerId: TimerId
) : WorkflowEngineMessage()

@Serializable
data class TaskFailed(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskName: TaskName,
    val taskError: TaskError
) : WorkflowEngineMessage()

@Serializable
data class TaskCanceled(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskName: TaskName
) : WorkflowEngineMessage()

@Serializable
data class TaskCompleted(
    override val workflowId: WorkflowId,
    override val workflowName: WorkflowName,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskName: TaskName,
    val taskReturnValue: MethodReturnValue
) : WorkflowEngineMessage()
