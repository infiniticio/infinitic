package com.zenaton.engine.events.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class EventId(override val id: String = UUID.randomUUID().toString()) : IdInterface
