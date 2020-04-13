package com.zenaton.engine.common.attributes

import java.util.UUID

data class WorkflowId(
    val uuid: String = UUID.randomUUID().toString()
)
