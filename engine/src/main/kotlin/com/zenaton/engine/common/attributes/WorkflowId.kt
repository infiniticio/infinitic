package com.zenaton.engine.common.attributes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class WorkflowId(val id: String = UUID.randomUUID().toString()) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = WorkflowId(value)
    }
    @JsonValue
    override fun toString() = id
}
