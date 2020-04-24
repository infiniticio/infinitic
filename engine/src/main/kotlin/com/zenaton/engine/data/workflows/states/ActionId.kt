package com.zenaton.engine.data.workflows.states

import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventId
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.types.Id
import com.zenaton.engine.data.workflows.WorkflowId

data class ActionId(override val id: String) : Id(id) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
