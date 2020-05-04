package com.zenaton.engine.delays.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class DelayId(override val id: String = UUID.randomUUID().toString()) : IdInterface
