package com.zenaton.engine.common.attributes

import java.util.UUID

data class DecisionAttemptId(
    val uuid: String = UUID.randomUUID().toString()
)
