package com.zenaton.engine.interfaces.data

import java.time.LocalDateTime
import java.time.ZoneOffset

data class DateTime(val time: Long = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond())
