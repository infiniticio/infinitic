package com.zenaton.jobManager.data

import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedOutput

data class JobOutput(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedOutput(serializedData, serializationType)
