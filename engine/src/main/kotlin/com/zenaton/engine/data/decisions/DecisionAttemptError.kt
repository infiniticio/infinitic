package com.zenaton.engine.data.decisions

import com.zenaton.engine.data.types.Data

data class DecisionAttemptError(override val data: ByteArray) : Data(data)
