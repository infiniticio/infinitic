package com.zenaton.engine.topics.workflows.state

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DelayId
import com.zenaton.engine.data.EventData
import com.zenaton.engine.data.EventId
import com.zenaton.engine.data.EventName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.WorkflowOutput
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskOutput

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
sealed class Action(
    open val decidedAt: DateTime,
    open val actionHash: ActionHash,
    open val actionStatus: ActionStatus
)

data class DispatchTask(
    val taskId: TaskId,
    var taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : Action(decidedAt, actionHash, actionStatus)

data class DispatchChildWorkflow(
    val childWorkflowId: WorkflowId,
    var childWorkflowOutput: WorkflowOutput?,
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : Action(decidedAt, actionHash, actionStatus)

data class WaitDelay(
    val delayId: DelayId,
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : Action(decidedAt, actionHash, actionStatus)

data class WaitEvent(
    val eventId: EventId,
    val eventName: EventName,
    var eventData: EventData?,
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : Action(decidedAt, actionHash, actionStatus)

/**
 * InstantTask have already been processed by the Decider
 */
data class InstantTask(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override val actionStatus: ActionStatus,
    var taskOutput: TaskOutput?
) : Action(decidedAt, actionHash, actionStatus)

/**
 * EngineAction are processed right away by the Engine
 */
sealed class EngineAction(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override val actionStatus: ActionStatus
) : Action(decidedAt, actionHash, actionStatus)

data class PauseWorkflow(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : EngineAction(decidedAt, actionHash, actionStatus)

data class ResumeWorkflow(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : EngineAction(decidedAt, actionHash, actionStatus)

data class CompleteWorkflow(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED,
    val workflowOutput: WorkflowOutput? = null
) : EngineAction(decidedAt, actionHash, actionStatus)

data class TerminateWorkflow(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED
) : EngineAction(decidedAt, actionHash, actionStatus)

data class SendEvent(
    override val decidedAt: DateTime,
    override val actionHash: ActionHash,
    override var actionStatus: ActionStatus = ActionStatus.DISPATCHED,
    val eventName: EventName,
    var eventData: EventData?
) : EngineAction(decidedAt, actionHash, actionStatus)
