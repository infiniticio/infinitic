package io.infinitic.workflowManager.common.messages

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.decisions.DecisionId
import io.infinitic.workflowManager.common.data.decisions.DecisionInput
import io.infinitic.workflowManager.common.data.decisions.DecisionOutput
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.EventData
import io.infinitic.workflowManager.common.data.EventName
import io.infinitic.workflowManager.common.data.WorkflowId
import io.infinitic.workflowManager.common.data.WorkflowInput
import io.infinitic.workflowManager.common.data.WorkflowMeta
import io.infinitic.workflowManager.common.data.WorkflowName
import io.infinitic.workflowManager.common.data.WorkflowOptions
import io.infinitic.workflowManager.common.data.branches.BranchOutput

sealed class Message

sealed class ForWorkflowEngineMessage(open val workflowId: WorkflowId) : Message()

data class CancelWorkflow(
    override val workflowId: WorkflowId,
    val workflowOutput: BranchOutput?
) : ForWorkflowEngineMessage(workflowId)

data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: BranchOutput?
) : ForWorkflowEngineMessage(workflowId)

data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: BranchOutput?
) : ForWorkflowEngineMessage(workflowId)

data class DecisionCompleted(
    override val workflowId: WorkflowId,
    val decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : ForWorkflowEngineMessage(workflowId)

data class DecisionDispatched(
    override val workflowId: WorkflowId,
    val decisionId: DecisionId,
    val workflowName: WorkflowName,
    val decisionInput: DecisionInput
) : ForWorkflowEngineMessage(workflowId)

data class DelayCompleted(
    override val workflowId: WorkflowId,
    val delayId: DelayId
) : ForWorkflowEngineMessage(workflowId)

data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowInput: WorkflowInput,
    val workflowMeta: WorkflowMeta,
    val workflowOptions: WorkflowOptions
) : ForWorkflowEngineMessage(workflowId)

data class EventReceived(
    override val workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : ForWorkflowEngineMessage(workflowId)

data class TaskCanceled(
    override val workflowId: WorkflowId,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : ForWorkflowEngineMessage(workflowId)

data class TaskCompleted(
    override val workflowId: WorkflowId,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : ForWorkflowEngineMessage(workflowId)

data class TaskDispatched(
    override val workflowId: WorkflowId,
    val taskId: TaskId,
    val taskInput: TaskInput?
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowOutput: TaskOutput?
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowOutput: TaskOutput?
) : ForWorkflowEngineMessage(workflowId)
