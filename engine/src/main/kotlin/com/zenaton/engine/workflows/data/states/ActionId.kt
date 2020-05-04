package com.zenaton.engine.workflows.data.states

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.events.data.EventId
import com.zenaton.engine.interfaces.data.IdInterface
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.workflows.data.WorkflowId
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : IdInterface {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
