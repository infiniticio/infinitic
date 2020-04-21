package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.engine.attributes.types.Hash

data class StoreHash(override val hash: String) : Hash(hash) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = StoreHash(value)
    }
    @JsonValue
    fun toJson() = hash
}
