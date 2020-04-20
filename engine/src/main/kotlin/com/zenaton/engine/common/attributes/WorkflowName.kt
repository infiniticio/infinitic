package com.zenaton.engine.common.attributes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class WorkflowName(val name: String) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = WorkflowName(value)
    }
    @JsonValue
    override fun toString() = name
}
