package com.zenaton.engine.data.decisions

import com.zenaton.engine.data.types.Data

data class DecisionOutput(override val data: ByteArray) : Data(data)
