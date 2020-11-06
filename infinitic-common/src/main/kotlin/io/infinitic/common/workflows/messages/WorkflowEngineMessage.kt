// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.messages

import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.DelayId
import io.infinitic.common.workflows.data.events.EventData
import io.infinitic.common.workflows.data.events.EventName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowEngineMessage() {
    abstract val workflowId: WorkflowId
}

@Serializable
data class CancelWorkflow(
    override val workflowId: WorkflowId,
    val workflowOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class WorkflowTaskCompleted(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowTaskOutput: WorkflowTaskOutput
) : WorkflowEngineMessage()

@Serializable
data class WorkflowTaskDispatched(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowName: WorkflowName,
    val workflowTaskInput: WorkflowTaskInput
) : WorkflowEngineMessage()

@Serializable
data class TimerCompleted(
    override val workflowId: WorkflowId,
    val delayId: DelayId
) : WorkflowEngineMessage()

@Serializable
data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var parentMethodRunId: MethodRunId? = null,
    val workflowName: WorkflowName,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodInput: MethodInput,
    val workflowMeta: WorkflowMeta,
    val workflowOptions: WorkflowOptions
) : WorkflowEngineMessage()

@Serializable
data class ObjectReceived(
    override val workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowEngineMessage()

@Serializable
data class TaskCanceled(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val methodOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class TaskCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class TaskDispatched(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val methodName: MethodName,
    val methodInput: MethodInput
) : WorkflowEngineMessage()

@Serializable
data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowOutput: MethodOutput
) : WorkflowEngineMessage()

@Serializable
data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowOutput: MethodOutput
) : WorkflowEngineMessage()
