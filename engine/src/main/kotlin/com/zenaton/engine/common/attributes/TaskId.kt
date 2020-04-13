package com.zenaton.engine.common.attributes

import java.io.Serializable
import java.util.UUID

data class TaskId(
    val uuid: String = UUID.randomUUID().toString()
) : Serializable
