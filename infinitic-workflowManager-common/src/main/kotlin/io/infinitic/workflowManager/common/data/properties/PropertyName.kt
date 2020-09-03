package io.infinitic.workflowManager.common.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class PropertyName
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val name: String) : Comparable<String> by name
