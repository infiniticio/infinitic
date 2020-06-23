package com.zenaton.workflowManager.data.properties

import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedData

data class PropertyData(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedData(serializedData, serializationType) {
    fun propertyHash() = PropertyHash(hash())
}
