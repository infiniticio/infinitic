package com.zenaton.jobManager.data

import com.zenaton.common.data.AvrosSerializationType
import com.zenaton.common.data.SerializedOutput

data class JobOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedOutput(serializedData, serializationType)
