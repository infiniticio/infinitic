package com.zenaton.engine.common.attributes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class WorkflowData(val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskData

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = WorkflowData(value.toByteArray(charset = Charsets.UTF_8))
    }
    @JsonValue
    override fun toString() = String(bytes = data, charset = Charsets.UTF_8)
}
