package com.zenaton.common.data

open class SerializedParameter(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedData(serializedData, serializationType)
