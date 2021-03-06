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
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowEngineMessage() {
    val messageId: MessageId = MessageId()
    abstract val workflowId: WorkflowId
}

@Serializable
data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    val clientName: ClientName,
    val clientWaiting: Boolean,
    var parentWorkflowId: WorkflowId?,
    var parentMethodRunId: MethodRunId?,
    val workflowName: WorkflowName,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val workflowMeta: WorkflowMeta,
    val workflowOptions: WorkflowOptions
) : WorkflowEngineMessage()

@Serializable
data class CancelWorkflow(
    override val workflowId: WorkflowId,
    val clientName: ClientName?,
    val workflowOutput: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class SendToChannel(
    val clientName: ClientName,
    val clientWaiting: Boolean,
    override val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val channelEventId: ChannelEventId,
    val channelName: ChannelName,
    val channelEvent: ChannelEvent
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childWorkflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class WorkflowTaskCompleted(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowTaskReturnValue: WorkflowTaskOutput
) : WorkflowEngineMessage()

@Serializable
data class WorkflowTaskDispatched(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowName: WorkflowName,
    val workflowTaskParameters: WorkflowTaskParameters
) : WorkflowEngineMessage()

@Serializable
data class TimerCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val timerId: TimerId
) : WorkflowEngineMessage()

@Serializable
data class TaskCanceled(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class TaskCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class TaskDispatched(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val methodName: MethodName,
    val methodParameters: MethodParameters
) : WorkflowEngineMessage()

@Serializable
data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()

@Serializable
data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowReturnValue: MethodReturnValue
) : WorkflowEngineMessage()
