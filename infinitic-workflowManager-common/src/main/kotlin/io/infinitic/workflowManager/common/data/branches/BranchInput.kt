package io.infinitic.workflowManager.common.data.branches

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Input
import java.lang.reflect.Method

class BranchInput(override vararg val data: Any?) : Input(data), Collection<Any?> by data.toList() {
    @get:JsonValue val json get() = getSerialized()

    companion object {
        @JvmStatic @JsonCreator
        fun fromSerialized(serialized: List<SerializedData>) =
            BranchInput(*(serialized.map { it.deserialize() }.toTypedArray())).apply { serializedData = serialized }

        fun from(m: Method, data: Array<out Any>) =
            BranchInput(*data).apply { serializedData = getSerialized(m) }
    }
}
