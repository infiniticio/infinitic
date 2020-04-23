package com.zenaton.engine.attributes.delays

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class DelayId(override val id: String = UUID.randomUUID().toString()) : Id(id)
