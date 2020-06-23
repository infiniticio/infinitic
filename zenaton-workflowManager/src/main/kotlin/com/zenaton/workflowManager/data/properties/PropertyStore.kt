package com.zenaton.workflowManager.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class PropertyStore
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val properties: MutableMap<PropertyHash, PropertyData>)
