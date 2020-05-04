package com.zenaton.engine.workflows.data.states

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class BranchId(override val id: String = UUID.randomUUID().toString()) : IdInterface
