package com.zenaton.workflowManager.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.interfaces.IdInterface
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventId
import com.zenaton.workflowManager.data.WorkflowId

data class CommandId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String) : IdInterface {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
