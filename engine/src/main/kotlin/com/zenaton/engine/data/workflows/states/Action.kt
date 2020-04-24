package com.zenaton.engine.data.workflows.states

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventData
import com.zenaton.engine.data.events.EventId
import com.zenaton.engine.data.events.EventName
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.types.DateTime
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowOutput

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
    var taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override var status: ActionStatus
) : Action(decidedAt, status)

data class InstantTask(
    val taskId: TaskId,
    val taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus = ActionStatus.COMPLETED
) : Action(decidedAt, status)

data class DispatchWorkflow(
    val workflowId: WorkflowId,
    var workflowOutput: WorkflowOutput?,
    override val decidedAt: DateTime,
    override var status: ActionStatus
) : Action(decidedAt, status)

data class DispatchDelay(
    val delayId: DelayId,
    override val decidedAt: DateTime,
    override var status: ActionStatus
) : Action(decidedAt, status)

data class ListenEvent(
    val eventId: EventId,
    val eventName: EventName,
    var eventData: EventData?,
    override val decidedAt: DateTime,
    override var status: ActionStatus
) : Action(decidedAt, status)
