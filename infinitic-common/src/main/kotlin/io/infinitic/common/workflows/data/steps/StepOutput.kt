package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.data.bases.Data

data class StepOutput(override val data: Any?) : Data(data) {
    @get:JsonValue
    val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            StepOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
