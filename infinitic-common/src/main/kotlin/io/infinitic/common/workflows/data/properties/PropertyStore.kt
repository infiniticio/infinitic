package io.infinitic.common.workflows.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class PropertyStore
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val properties: MutableMap<PropertyHash, PropertyValue> = mutableMapOf()) : Map<PropertyHash, PropertyValue> by properties
