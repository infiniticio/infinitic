package com.zenaton.engine.data.events

import com.zenaton.engine.data.types.Name

data class EventName(override val name: String) : Name(name)
