package com.zenaton.engine.common.attributes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class DecisionId(val id: String = UUID.randomUUID().toString()) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String) = DecisionId(value)
    }
    @JsonValue
    override fun toString() = id
}
