package com.zenaton.workflowManager.data

import com.zenaton.common.data.AvroSerializationType
import com.zenaton.common.data.interfaces.SerializedDataInterface

class DecisionOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedDataInterface {
    override fun equals(other: Any?) = equalsData(other)
    override fun hashCode() = hashCodeData()
}
