package com.zenaton.engine.attributes.tasks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Name

data class TaskName(override val name: String) : Name(name) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = TaskName(value)
    }
    @JsonValue
    fun toJson() = name
}
