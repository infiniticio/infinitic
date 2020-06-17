package com.zenaton.jobManager.data

import com.zenaton.commons.data.AvrosSerializationType
import com.zenaton.commons.data.interfaces.SerializedDataInterface

data class JobOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedDataInterface {
    override fun equals(other: Any?) = equalsData(other)
    override fun hashCode() = hashCodeData()
}
