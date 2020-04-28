package com.zenaton.engine.data.workflows.states

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class BranchId(override val id: String = UUID.randomUUID().toString()) : Id(id)
