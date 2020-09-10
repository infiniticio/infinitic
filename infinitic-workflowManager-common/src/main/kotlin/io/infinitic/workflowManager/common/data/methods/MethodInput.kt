package io.infinitic.workflowManager.common.data.methods

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.bases.Input
import java.lang.reflect.Method

class MethodInput(override vararg val data: Any?) : Input(data), Collection<Any?> by data.toList() {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator
        fun fromSerialized(serialized: List<SerializedData>) =
            MethodInput(*(serialized.map { it.deserialize() }.toTypedArray())).apply { serializedData = serialized }

        fun from(m: Method, data: Array<out Any>) =
            MethodInput(*data).apply { serializedData = getSerialized(m) }
    }
}
