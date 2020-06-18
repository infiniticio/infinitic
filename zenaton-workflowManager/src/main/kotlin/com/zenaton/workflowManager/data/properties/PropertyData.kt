package com.zenaton.workflowManager.data.properties

import com.zenaton.common.data.AvroSerializationType
import com.zenaton.common.data.interfaces.SerializedDataInterface

class PropertyData(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedDataInterface {
    override fun equals(other: Any?) = equalsData(other)
    override fun hashCode() = hashCodeData()
    fun propertyHash() = PropertyHash(hash())
}
