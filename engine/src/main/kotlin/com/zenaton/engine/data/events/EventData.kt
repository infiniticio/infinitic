package com.zenaton.engine.data.events

import com.zenaton.engine.data.types.Data

data class EventData(override val data: ByteArray) : Data(data)
