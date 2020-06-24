package com.zenaton.common.data

abstract class SerializedOutput(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedData(serializedData, serializationType)
