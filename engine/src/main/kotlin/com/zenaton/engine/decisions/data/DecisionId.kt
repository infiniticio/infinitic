package com.zenaton.engine.decisions.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class DecisionId(override val id: String = UUID.randomUUID().toString()) : IdInterface
