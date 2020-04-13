package com.zenaton.engine.common.attributes

import java.util.*

data class DecisionId (
    val uuid: String = UUID.randomUUID().toString()
)
