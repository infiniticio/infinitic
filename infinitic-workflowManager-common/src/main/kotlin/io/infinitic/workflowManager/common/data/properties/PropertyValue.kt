package io.infinitic.workflowManager.common.data.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.bases.Data

data class PropertyValue(override val data: Any?) : Data(data) {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            PropertyValue(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }

    fun hash() = PropertyHash(json.hash())
}
