package com.zenaton.engine.events.data

import com.zenaton.engine.interfaces.data.DataInterface

data class EventData(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
