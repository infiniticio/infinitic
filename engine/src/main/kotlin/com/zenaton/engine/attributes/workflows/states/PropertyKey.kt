package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class PropertyKey(val key: String) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = PropertyKey(key = value)
    }
    @JsonValue
    fun toJson() = key
}
