package com.zenaton.common.data

open class SerializedParameter(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedData(serializedData, serializationType)
