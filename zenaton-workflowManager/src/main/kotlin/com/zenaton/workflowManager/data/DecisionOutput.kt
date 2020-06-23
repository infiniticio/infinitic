package com.zenaton.workflowManager.data

import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedOutput

data class DecisionOutput(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedOutput(serializedData, serializationType)
