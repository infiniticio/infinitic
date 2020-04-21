package com.zenaton.engine.attributes.workflows

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Name

data class WorkflowName(override val name: String) : Name(name) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = WorkflowName(value)
    }
    @JsonValue
    fun toJson() = name
}
