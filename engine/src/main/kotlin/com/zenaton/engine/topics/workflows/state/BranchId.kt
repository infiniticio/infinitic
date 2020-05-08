package com.zenaton.engine.topics.workflows.state

import com.zenaton.engine.data.interfaces.IdInterface
import java.util.UUID

data class BranchId(override val id: String = UUID.randomUUID().toString()) : IdInterface
