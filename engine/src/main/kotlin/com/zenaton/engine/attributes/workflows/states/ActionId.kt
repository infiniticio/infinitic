package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.delays.DelayId
import com.zenaton.engine.attributes.events.EventId
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.types.Id
import com.zenaton.engine.attributes.workflows.WorkflowId
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : Id(id) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)

    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = ActionId(value)
    }
    @JsonValue
    fun toJson() = id
}
