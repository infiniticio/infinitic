package com.zenaton.engine.topics.workflows.state

import com.zenaton.engine.data.interfaces.DataInterface

data class PropertyData(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
    fun propertyHash() = PropertyHash(hash())
}
