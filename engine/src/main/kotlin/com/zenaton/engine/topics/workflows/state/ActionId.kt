package com.zenaton.engine.topics.workflows.state

import com.zenaton.engine.data.DelayId
import com.zenaton.engine.data.EventId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.interfaces.IdInterface
import com.zenaton.taskmanager.data.TaskId
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : IdInterface {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
