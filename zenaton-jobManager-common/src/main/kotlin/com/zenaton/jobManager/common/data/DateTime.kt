package com.zenaton.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.time.ZoneOffset

data class DateTime
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue val time: Long = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond())
