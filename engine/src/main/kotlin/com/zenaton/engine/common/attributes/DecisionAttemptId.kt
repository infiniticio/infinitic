package com.zenaton.engine.common.attributes

import java.util.*

data class DecisionAttemptId (
    val uuid: String = UUID.randomUUID().toString()
)
