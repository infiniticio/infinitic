package com.zenaton.engine.attributes.decisions

import com.zenaton.engine.attributes.types.Data

data class DecisionAttemptError(override val data: ByteArray) : Data(data)
