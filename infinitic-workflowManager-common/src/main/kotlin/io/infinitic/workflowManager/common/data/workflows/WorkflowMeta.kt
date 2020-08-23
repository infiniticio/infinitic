package io.infinitic.workflowManager.common.data.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Meta

data class WorkflowMeta(override val data: MutableMap<String, Any?> = mutableMapOf()) : Meta(data), MutableMap<String, Any?> by data {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromSerialized(serialized: Map<String, SerializedData>) =
            WorkflowMeta(deserialize(serialized)).apply { serializedData = serialized }
    }
}
