package com.zenaton.engine.data.workflows

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class WorkflowId(override val id: String = UUID.randomUUID().toString()) : Id(id)
