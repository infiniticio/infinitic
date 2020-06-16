package com.zenaton.jobManager.data

import com.zenaton.commons.data.SerializationType
import com.zenaton.commons.data.interfaces.SerializedDataInterface

data class JobParameter(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedDataInterface {
    override fun equals(other: Any?) = equalsData(other)
    override fun hashCode() = hashCodeData()
}
