package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Data

data class PropertyData(override val data: ByteArray) : Data(data) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) =
            PropertyData(value.toByteArray(charset = Charsets.UTF_8))
    }
    @JsonValue
    fun toJson() = String(bytes = data, charset = Charsets.UTF_8)
}
