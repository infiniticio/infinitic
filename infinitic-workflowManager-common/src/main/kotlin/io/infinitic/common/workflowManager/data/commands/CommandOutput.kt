package io.infinitic.common.workflowManager.data.commands

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.common.taskManager.data.bases.Data

data class CommandOutput(override val data: Any?) : Data(data) {
    @get:JsonValue
    val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            CommandOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
