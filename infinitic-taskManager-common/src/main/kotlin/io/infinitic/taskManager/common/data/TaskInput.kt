package io.infinitic.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import java.lang.reflect.Method

class TaskInput(override vararg val data: Any?) : Input(data), Collection<Any?> by data.toList() {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator
        fun fromSerialized(serialized: List<SerializedData>) =
            TaskInput(*deserialize(serialized)).apply { serializedData = serialized }

        fun from(method: Method, data: Array<out Any>) =
            TaskInput(*data).apply { serializedData = getSerialized(method) }
    }
}
