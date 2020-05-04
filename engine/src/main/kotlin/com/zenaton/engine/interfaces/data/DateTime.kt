package com.zenaton.engine.interfaces.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.time.ZoneOffset

data class DateTime(val time: Long = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond()) {
    companion object {
        @JvmStatic @JsonCreator
        fun fromJson(value: String) = DateTime(value.toLong())
    }
    @JsonValue
    override fun toString() = time.toString()
}
