package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Id
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.DelayId
import io.infinitic.common.workflows.data.events.EventId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import java.util.UUID

data class CommandId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String = UUID.randomUUID().toString()) : Id(id) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
