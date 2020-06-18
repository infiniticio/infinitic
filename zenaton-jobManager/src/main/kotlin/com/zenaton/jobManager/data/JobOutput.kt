package com.zenaton.jobManager.data

import com.zenaton.common.data.AvroSerializationType
import com.zenaton.common.data.SerializedOutput

data class JobOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedOutput(serializedData, serializationType)
