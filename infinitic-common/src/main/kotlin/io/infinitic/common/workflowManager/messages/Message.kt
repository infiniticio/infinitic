package io.infinitic.common.workflowManager.messages

import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskOutput
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflowManager.data.DelayId
import io.infinitic.common.workflowManager.data.events.EventData
import io.infinitic.common.workflowManager.data.events.EventName
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.methodRuns.MethodInput
import io.infinitic.common.workflowManager.data.workflows.WorkflowMeta
import io.infinitic.common.workflowManager.data.workflows.WorkflowName
import io.infinitic.common.workflowManager.data.workflows.WorkflowOptions
import io.infinitic.common.workflowManager.data.methodRuns.MethodOutput
import io.infinitic.common.workflowManager.data.methodRuns.MethodName
import io.infinitic.common.workflowManager.data.methodRuns.MethodRunId

sealed class Message

sealed class ForWorkflowEngineMessage(open val workflowId: WorkflowId) : Message()

data class CancelWorkflow(
    override val workflowId: WorkflowId,
    val workflowOutput: MethodOutput
) : ForWorkflowEngineMessage(workflowId)

data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: MethodOutput
) : ForWorkflowEngineMessage(workflowId)

data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: MethodOutput
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowTaskCompleted(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowTaskOutput: WorkflowTaskOutput
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowTaskDispatched(
    override val workflowId: WorkflowId,
    val workflowTaskId: WorkflowTaskId,
    val workflowName: WorkflowName,
    val workflowTaskInput: WorkflowTaskInput
) : ForWorkflowEngineMessage(workflowId)

data class TimerCompleted(
    override val workflowId: WorkflowId,
    val delayId: DelayId
) : ForWorkflowEngineMessage(workflowId)

data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var parentMethodRunId: MethodRunId? = null,
    val workflowName: WorkflowName,
    val methodName: MethodName,
    val methodInput: MethodInput,
    val workflowMeta: WorkflowMeta,
    val workflowOptions: WorkflowOptions
) : ForWorkflowEngineMessage(workflowId)

data class ObjectReceived(
    override val workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : ForWorkflowEngineMessage(workflowId)

data class TaskCanceled(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskOutput: TaskOutput
) : ForWorkflowEngineMessage(workflowId)

data class TaskCompleted(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskOutput: TaskOutput
) : ForWorkflowEngineMessage(workflowId)

data class TaskDispatched(
    override val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val taskId: TaskId,
    val taskInput: TaskInput?
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowOutput: TaskOutput
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowOutput: MethodOutput
) : ForWorkflowEngineMessage(workflowId)
