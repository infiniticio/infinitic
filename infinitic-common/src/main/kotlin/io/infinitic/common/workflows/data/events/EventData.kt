package io.infinitic.common.workflows.data.events

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData

data class EventData
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val input: List<SerializedData>)
