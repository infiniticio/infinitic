package io.infinitic.workflowManager.engine.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.interfaces.IdInterface
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.engine.data.DelayId
import io.infinitic.workflowManager.engine.data.EventId
import io.infinitic.workflowManager.engine.data.WorkflowId

data class CommandId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String) : IdInterface {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
