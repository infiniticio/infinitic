package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.attributes.delays.DelayId
import com.zenaton.engine.attributes.events.EventData
import com.zenaton.engine.attributes.events.EventId
import com.zenaton.engine.attributes.events.EventName
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.tasks.TaskOutput
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowOutput

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DispatchTask::class, name = "DISPATCH_TASK"),
    JsonSubTypes.Type(value = InstantTask::class, name = "INSTANT_TASK"),
    JsonSubTypes.Type(value = DispatchWorkflow::class, name = "DISPATCH_WORKFLOW"),
    JsonSubTypes.Type(value = DispatchDelay::class, name = "DISPATCH_DELAY"),
    JsonSubTypes.Type(value = ListenEvent::class, name = "LISTEN_EVENT")
)
sealed class Action(
    open val decidedAt: DateTime,
    open val status: ActionStatus
)

data class DispatchTask(
    val taskId: TaskId,
    val taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(decidedAt, status)

data class InstantTask(
    val taskId: TaskId,
    val taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus = ActionStatus.COMPLETED
) : Action(decidedAt, status)

data class DispatchWorkflow(
    val workflowId: WorkflowId,
    val workflowOutput: WorkflowOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(decidedAt, status)

data class DispatchDelay(
    val delayId: DelayId,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(decidedAt, status)

data class ListenEvent(
    val eventId: EventId,
    val eventName: EventName,
    val eventData: EventData?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(decidedAt, status)
