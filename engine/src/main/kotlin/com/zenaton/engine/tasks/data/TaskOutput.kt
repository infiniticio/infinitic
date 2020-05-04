package com.zenaton.engine.tasks.data

import com.zenaton.engine.interfaces.data.DataInterface

data class TaskOutput(override val data: ByteArray) :
    DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
