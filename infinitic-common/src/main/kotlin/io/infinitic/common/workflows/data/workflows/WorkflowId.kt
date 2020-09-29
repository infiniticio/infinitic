package io.infinitic.common.workflows.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Id
import java.util.UUID

data class WorkflowId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String = UUID.randomUUID().toString()) : Id(id)
