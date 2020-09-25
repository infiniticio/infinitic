package io.infinitic.common.workflowManager.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.taskManager.data.bases.Id
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.workflowManager.data.DelayId
import io.infinitic.common.workflowManager.data.events.EventId
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import java.util.UUID

data class CommandId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String = UUID.randomUUID().toString()) : Id(id) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
