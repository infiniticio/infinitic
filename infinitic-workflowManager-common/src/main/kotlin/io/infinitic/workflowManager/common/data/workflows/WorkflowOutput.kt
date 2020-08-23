package io.infinitic.workflowManager.common.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Output

data class WorkflowOutput(override val data: Any?) : Output(data) {
    @get:JsonValue
    val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serializedData: SerializedData) =
            WorkflowOutput(serializedData.deserialize()).apply {
                this.serializedData = serializedData
            }
    }
}
