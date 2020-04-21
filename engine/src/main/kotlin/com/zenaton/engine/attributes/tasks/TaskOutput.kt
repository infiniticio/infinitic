package com.zenaton.engine.attributes.tasks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Data

data class TaskOutput(override val data: ByteArray) : Data(data) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) =
            TaskOutput(value.toByteArray(charset = Charsets.UTF_8))
    }
    @JsonValue
    fun toJson() = String(bytes = data, charset = Charsets.UTF_8)
}
