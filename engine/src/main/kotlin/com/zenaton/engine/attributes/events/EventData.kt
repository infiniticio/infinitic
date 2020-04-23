package com.zenaton.engine.attributes.events

import com.zenaton.engine.attributes.types.Data

data class EventData(override val data: ByteArray) : Data(data)
