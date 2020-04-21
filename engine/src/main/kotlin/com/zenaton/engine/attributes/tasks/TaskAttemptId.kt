package com.zenaton.engine.attributes.tasks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class TaskAttemptId(override val id: String = UUID.randomUUID().toString()) : Id(id) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = TaskAttemptId(value)
    }
    @JsonValue
    fun toJson() = id
}
