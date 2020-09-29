package io.infinitic.common.workflows.data.events

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.tasks.data.bases.Id
import java.util.UUID

data class EventId
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val id: String = UUID.randomUUID().toString()) : Id(id)
