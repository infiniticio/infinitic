package com.zenaton.engine.decisionAttempts.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class DecisionAttemptId(override val id: String = UUID.randomUUID().toString()) : IdInterface
