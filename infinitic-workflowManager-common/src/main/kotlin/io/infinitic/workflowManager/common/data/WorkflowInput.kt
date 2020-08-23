package io.infinitic.workflowManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Input
import java.lang.reflect.Method

class WorkflowInput(override vararg val data: Any?) : Input(data) {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator
        fun fromSerialized(s: List<SerializedData>) =
            WorkflowInput(*(s.map { it.deserialize() }.toTypedArray())).apply { serialized = s }

        fun from(m: Method, data: Array<out Any>) =
            WorkflowInput(*data).apply { serialized = getSerialized(m) }
    }
}
