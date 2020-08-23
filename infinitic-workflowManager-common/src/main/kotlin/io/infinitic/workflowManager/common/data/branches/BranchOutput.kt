package io.infinitic.workflowManager.common.data.branches

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Output

data class BranchOutput(override val data: Any?) : Output(data) {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            BranchOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
