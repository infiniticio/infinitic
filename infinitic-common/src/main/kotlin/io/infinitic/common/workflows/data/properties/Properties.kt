package io.infinitic.common.workflows.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Properties
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val properties: MutableMap<PropertyName, PropertyHash> = mutableMapOf()) : MutableMap<PropertyName, PropertyHash> by properties
