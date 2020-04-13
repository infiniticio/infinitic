package com.zenaton.engine.common.attributes

import java.util.*

data class TaskAttemptId (
    val uuid: String = UUID.randomUUID().toString()
)
