package com.zenaton.engine.attributes.decisions

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class DecisionAttemptId(override val id: String = UUID.randomUUID().toString()) : Id(id)
