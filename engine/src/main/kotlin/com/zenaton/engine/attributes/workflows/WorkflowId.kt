package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class WorkflowId(override val id: String = UUID.randomUUID().toString()) : Id(id)
