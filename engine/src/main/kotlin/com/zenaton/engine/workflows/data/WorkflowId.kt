package com.zenaton.engine.workflows.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class WorkflowId(override val id: String = UUID.randomUUID().toString()) : IdInterface
