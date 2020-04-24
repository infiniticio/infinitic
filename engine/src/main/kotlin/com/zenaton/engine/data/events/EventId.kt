package com.zenaton.engine.data.events

import com.zenaton.engine.data.types.Id
import java.util.UUID

data class EventId(override val id: String = UUID.randomUUID().toString()) : Id(id)
