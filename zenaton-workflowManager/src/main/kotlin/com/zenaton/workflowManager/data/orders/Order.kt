package com.zenaton.workflowManager.data.orders

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventData
import com.zenaton.workflowManager.data.EventId
import com.zenaton.workflowManager.data.EventName
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.WorkflowName
import com.zenaton.workflowManager.data.branches.BranchId
import com.zenaton.workflowManager.data.branches.BranchInput
import com.zenaton.workflowManager.data.branches.BranchOutput
import com.zenaton.workflowManager.data.branches.BranchPosition

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = DispatchChildWorkflow::class, name = "DISPATCH_CHILD_WORKFLOW"),
    JsonSubTypes.Type(value = WaitDelay::class, name = "WAIT_DELAY"),
    JsonSubTypes.Type(value = WaitEvent::class, name = "WAIT_EVENT"),
    JsonSubTypes.Type(value = InstantTask::class, name = "INSTANT_TASK"),
    JsonSubTypes.Type(value = PauseWorkflow::class, name = "PAUSE_WORKFLOW"),
    JsonSubTypes.Type(value = ResumeWorkflow::class, name = "RESUME_WORKFLOW"),
    JsonSubTypes.Type(value = CompleteWorkflow::class, name = "COMPLETE_WORKFLOW"),
    JsonSubTypes.Type(value = TerminateWorkflow::class, name = "TERMINATE_WORKFLOW"),
    JsonSubTypes.Type(value = SendEvent::class, name = "SEND_EVENT")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Order(
    open val branchId: BranchId,
    open val branchPosition: BranchPosition
)

data class DispatchTask(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val taskId: JobId,
    val taskName: JobName,
    val taskMethod: JobMethod,
    var taskInput: JobInput
) : Order(branchId, branchPosition)

data class DispatchChildWorkflow(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val childWorkflowId: WorkflowId,
    val childWorkflowName: WorkflowName,
    val childWorkflowMethod: WorkflowMethod,
    var childWorkflowInput: BranchInput
) : Order(branchId, branchPosition)

data class WaitDelay(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val delayId: DelayId,
    val duration: Float
) : Order(branchId, branchPosition)

data class WaitEvent(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val eventId: EventId,
    val eventName: EventName
) : Order(branchId, branchPosition)

/**
 * InstantTask have already been processed by the Decider
 */
data class InstantTask(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    var instantTaskId: JobId,
    var instantTaskOutput: JobOutput
) : Order(branchId, branchPosition)

/**
 * EngineAction are processed right away by the Engine
 */
sealed class EngineAction(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition
) : Order(branchId, branchPosition)

data class PauseWorkflow(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val workflowId: WorkflowId
) : EngineAction(branchId, branchPosition)

data class ResumeWorkflow(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val workflowId: WorkflowId
) : EngineAction(branchId, branchPosition)

data class CompleteWorkflow(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val workflowId: WorkflowId,
    val workflowOutput: BranchOutput
) : EngineAction(branchId, branchPosition)

data class TerminateWorkflow(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val workflowId: WorkflowId,
    val workflowOutput: BranchOutput
) : EngineAction(branchId, branchPosition)

data class SendEvent(
    override val branchId: BranchId,
    override val branchPosition: BranchPosition,
    val workflowId: WorkflowId,
    val eventName: EventName,
    var eventData: EventData?
) : EngineAction(branchId, branchPosition)
