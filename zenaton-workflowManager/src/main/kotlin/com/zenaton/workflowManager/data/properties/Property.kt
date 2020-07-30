package com.zenaton.workflowManager.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.interfaces.PropertyInterface

data class Property
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val property: SerializedData) : PropertyInterface {
    fun propertyHash() = PropertyHash(property.hash())
}
