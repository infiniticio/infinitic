package com.zenaton.workflowManager.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Properties
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val properties: Map<PropertyKey, PropertyHash>)
