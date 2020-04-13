package com.zenaton.engine.common.attributes

import java.util.UUID

data class TaskAttemptId(
    val uuid: String = UUID.randomUUID().toString()
)
