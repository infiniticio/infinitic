package com.zenaton.workflowManager.data

import com.zenaton.common.data.AvroSerializationType
import com.zenaton.common.data.SerializedOutput

data class DecisionOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedOutput(serializedData, serializationType)
