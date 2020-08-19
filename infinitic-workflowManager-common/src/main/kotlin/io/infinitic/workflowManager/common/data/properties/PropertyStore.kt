package io.infinitic.workflowManager.common.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.workflowManager.common.data.properties.Property

data class PropertyStore
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val properties: MutableMap<PropertyHash, Property>)
