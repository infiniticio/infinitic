package com.zenaton.workflowManager.messages

import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.workflowManager.data.decisions.DecisionId
import com.zenaton.workflowManager.data.decisions.DecisionInput
import com.zenaton.workflowManager.data.decisions.DecisionOutput
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventData
import com.zenaton.workflowManager.data.EventName
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.WorkflowName
import com.zenaton.workflowManager.data.branches.BranchInput
import com.zenaton.workflowManager.data.branches.BranchOutput

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
    val workflowInput: BranchInput
) : ForWorkflowEngineMessage(workflowId)

data class EventReceived(
    override val workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : ForWorkflowEngineMessage(workflowId)

data class TaskCanceled(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskOutput: JobOutput?
) : ForWorkflowEngineMessage(workflowId)

data class TaskCompleted(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskOutput: JobOutput?
) : ForWorkflowEngineMessage(workflowId)

data class TaskDispatched(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskInput: JobInput?
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowOutput: JobOutput?
) : ForWorkflowEngineMessage(workflowId)

data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowOutput: JobOutput?
) : ForWorkflowEngineMessage(workflowId)
