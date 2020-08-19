package io.infinitic.workflowManager.engine.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.interfaces.PropertyInterface

data class Property
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val property: SerializedData) : PropertyInterface {
    fun hash() = PropertyHash(property.hash())
}
