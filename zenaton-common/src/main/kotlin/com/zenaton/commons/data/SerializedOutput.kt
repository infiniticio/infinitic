package com.zenaton.common.data

open class SerializedOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedData(serializedData, serializationType)
