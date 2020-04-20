package com.zenaton.engine.common.attributes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class TaskId(val id: String = UUID.randomUUID().toString()) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = TaskId(value)
    }
    @JsonValue
    override fun toString() = id
}
