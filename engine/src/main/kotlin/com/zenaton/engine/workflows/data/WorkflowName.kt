package com.zenaton.engine.workflows.data

import com.zenaton.engine.interfaces.data.NameInterface
import java.util.UUID

data class WorkflowName(override val name: String = UUID.randomUUID().toString()) : NameInterface
