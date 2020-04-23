package com.zenaton.engine.attributes.events

import com.zenaton.engine.attributes.types.Id
import java.util.UUID

data class EventId(override val id: String = UUID.randomUUID().toString()) : Id(id)
