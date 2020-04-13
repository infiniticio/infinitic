package com.zenaton.engine.common.attributes

import java.util.UUID

data class DelayId (
    val uuid: String = UUID.randomUUID().toString()
)
