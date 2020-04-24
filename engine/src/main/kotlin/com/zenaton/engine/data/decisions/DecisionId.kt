package com.zenaton.engine.data.decisions

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class DecisionId(override val id: String = UUID.randomUUID().toString()) : Id(id)
