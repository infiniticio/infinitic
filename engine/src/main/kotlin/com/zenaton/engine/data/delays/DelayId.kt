package com.zenaton.engine.data.delays

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class DelayId(override val id: String = UUID.randomUUID().toString()) : Id(id)
