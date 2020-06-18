package com.zenaton.jobManager.data

import com.zenaton.commons.data.AvrosSerializationType
import com.zenaton.commons.data.SerializedOutput

data class JobOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedOutput(serializedData, serializationType)
