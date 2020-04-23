package com.zenaton.engine.attributes.decisions

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class DecisionId(override val id: String = UUID.randomUUID().toString()) : Id(id)
