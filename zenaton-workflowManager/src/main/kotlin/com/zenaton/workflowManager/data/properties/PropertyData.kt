package com.zenaton.workflowManager.data.properties

import com.zenaton.common.data.AvrosSerializationType
import com.zenaton.common.data.interfaces.SerializedDataInterface

class PropertyData(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedDataInterface {
    override fun equals(other: Any?) = equalsData(other)
    override fun hashCode() = hashCodeData()
    fun propertyHash() = PropertyHash(hash())
}
