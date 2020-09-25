package io.infinitic.common.taskManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.common.taskManager.data.bases.Data

data class TaskOutput(override val data: Any?) : Data(data) {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            TaskOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
