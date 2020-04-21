package com.zenaton.engine.attributes.events

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Name

data class EventName(override val name: String) : Name(name) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = EventName(value)
    }
    @JsonValue
    fun toJson() = name
}
