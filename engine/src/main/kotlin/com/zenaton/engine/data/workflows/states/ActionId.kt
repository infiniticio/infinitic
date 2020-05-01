package com.zenaton.engine.data.workflows.states

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventId
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.workflows.WorkflowId
import java.util.UUID

data class ActionId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue val id: String = UUID.randomUUID().toString()) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
