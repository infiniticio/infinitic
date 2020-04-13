package com.zenaton.engine.common.attributes

import java.util.UUID

data class DecisionId(
    val uuid: String = UUID.randomUUID().toString()
)
