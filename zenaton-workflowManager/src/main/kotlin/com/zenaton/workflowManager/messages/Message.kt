package com.zenaton.workflowManager.messages

import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.workflowManager.data.DecisionData
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.DecisionOutput
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventData
import com.zenaton.workflowManager.data.EventName
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.WorkflowName
import com.zenaton.workflowManager.data.branches.BranchInput
import com.zenaton.workflowManager.data.branches.BranchOutput
import com.zenaton.workflowManager.messages.envelopes.ForDecidersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

sealed class Message

data class CancelWorkflow(
    override val workflowId: WorkflowId,
    val workflowOutput: BranchOutput?
) : Message(), ForWorkflowEngineMessage

data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: BranchOutput?
) : Message(), ForWorkflowEngineMessage

data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childBranchOutput: BranchOutput?
) : Message(), ForWorkflowEngineMessage

data class DecisionCompleted(
    override val workflowId: WorkflowId,
    val decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : Message(), ForWorkflowEngineMessage

data class DecisionDispatched(
    override val workflowId: WorkflowId,
    val decisionId: DecisionId,
    val workflowName: WorkflowName,
    val decisionData: DecisionData
) : Message(), ForWorkflowEngineMessage

data class DelayCompleted(
    override val workflowId: WorkflowId,
    val delayId: DelayId
) : Message(), ForWorkflowEngineMessage

data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowInput: BranchInput
) : Message(), ForWorkflowEngineMessage

data class EventReceived(
    override val workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : Message(), ForWorkflowEngineMessage

data class TaskCanceled(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskOutput: JobOutput?
) : Message(), ForWorkflowEngineMessage

data class TaskCompleted(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskOutput: JobOutput?
) : Message(), ForWorkflowEngineMessage

data class TaskDispatched(
    override val workflowId: WorkflowId,
    val taskId: JobId,
    val taskInput: JobInput?
) : Message(), ForWorkflowEngineMessage

data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    val workflowOutput: JobOutput?
) : Message(), ForWorkflowEngineMessage

data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    val workflowOutput: JobOutput?
) : Message(), ForWorkflowEngineMessage

data class DispatchTask(
    override val taskId: JobId,
    val workflowId: WorkflowId,
    val taskName: JobName,
    val taskData: JobInput
) : Message(), ForWorkersMessage

data class DispatchDecision(
    override val decisionId: DecisionId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val decisionData: DecisionData
) : Message(), ForDecidersMessage
