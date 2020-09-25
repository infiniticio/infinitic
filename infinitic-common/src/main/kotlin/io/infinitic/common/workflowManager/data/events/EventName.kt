package io.infinitic.common.workflowManager.data.events

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class EventName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val name: String)
