package io.infinitic.common.workflows.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.data.bases.Data

data class WorkflowOutput(override val data: Any?) : Data(data) {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            WorkflowOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
